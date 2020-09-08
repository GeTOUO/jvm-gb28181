package com.getouo.gb.scl.io

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

import com.getouo.gb.scl.data.{H264NaluData, PESFrame, PESH264IFrame, PESH264PFrame, PSH264Audio, PSH264Data, PSHeaders}
import com.getouo.gb.scl.rtp.RtpHeader
import com.getouo.gb.scl.server.RealtimeMediaStreamServer
import com.getouo.gb.scl.util.H264NALUFramer.START_TAG4
import com.getouo.gb.scl.util.{ByteLoserReader, ChannelUtil, H264NALUFramer}
import com.getouo.gb.util.BytesPrinter
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil
import io.pkts.buffer.Buffers

class GB28181RealtimeTCPSource() extends ChannelInboundHandlerAdapter with ActiveSource[PSH264Data] {
  val streamChannel = new RealtimeMediaStreamServer(this)
  streamChannel.run()
  val sdpInfo = new AtomicReference[String]("")

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info(s"收到流媒体客户端连接: ${ctx.channel().remoteAddress()}, " +
      s"remoteIp=${ChannelUtil.remoteIp(ctx.channel())}, remotePort=${ChannelUtil.castSocketAddr(ctx.channel().remoteAddress()).getPort}")
  }

  private def toReader(buf: ByteBuf): ByteLoserReader = {
    val readableLen = buf.readableBytes
    val fullBytes = new Array[Byte](readableLen)
    buf.readBytes(fullBytes)
    ByteLoserReader(fullBytes)
  }

  val frameBuf = new AtomicReference[PSH264Data](null)
  private val frameDeque: ConcurrentLinkedDeque[PSH264Data] = new ConcurrentLinkedDeque[PSH264Data]()

  /**
   * 收到经过 LengthFieldBasedFrameDecoder 处理的数据。
   *
   * @param ctx
   * @param msg
   */
  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    val loserReader = toReader(msg.asInstanceOf[ByteBuf])
    val arrayBuf = loserReader.toArray
    val totalLength = loserReader.size
    if (totalLength <= 14) return // 国标的rtp over tcp 具有2个字节长度头和12字节的rtp头。总长度小于这个值表面无有效数据
    val len = Buffers.wrap(loserReader.take(2).toArray).readUnsignedShort()

    val rtpHeader = RtpHeader(loserReader.take(12).toArray)
    val sequenceNumber = rtpHeader.sequenceNumber

    val hasPSHeader = loserReader.matcher(0, PSHeaders.PS_HEADER)
    val isAudio = loserReader.matcher(0, PSHeaders.PS_AUDIO_PES_HEADER)

    if (totalLength > 18 && hasPSHeader) { // 有ps头,进一步判断是否是i帧或者p帧
      //      val psHeader = loserReader.take(14)
      //      val stuffingLen = psHeader.last & 0x07
      //      val stuffingLength: Int = loserReader(13) & 0x07
      //      val startIndex = 14 + stuffingLength

      // ps 头前14字节是固定的， [第14字节 & 0x07] = 得到扩展的长度
      val sysHeaderStartIndex = 14 + (loserReader(13) & 0x07)
      val isIFrame = loserReader.length > 13 && loserReader.matcher(sysHeaderStartIndex, PSHeaders.PS_SYSTEM_HEADER_I_FRAME)

      if (isIFrame) {
        loserReader.drop(sysHeaderStartIndex + 4) // 丢弃 system header tag及之前的数据
        val sysHeaderLen = Buffers.wrap(loserReader.take(2).toArray).readUnsignedShort() // header tag后两个字节的长度，
        loserReader.drop(sysHeaderLen) // 丢弃 system头数据及之前的数据

        loserReader.drop(4) // 丢弃 system map tag
        val sysMapLen = Buffers.wrap(loserReader.take(2).toArray).readUnsignedShort() // header tag后两个字节的长度，
        loserReader.drop(sysMapLen) // 丢弃 system map 数据

        // pes 开始:
        val pts = getPts(loserReader)
        val iFrame = PESH264IFrame()
        loadFrameH264(loserReader, iFrame)
        frameDeque.add(iFrame)

        //        logger.info(
        //          s"""
        //             |一个iiiiiiiiiiiiiiiiiii帧数据:----------------------------------
        //             |总数据包长度=$totalLength; 剩余reader长度=${loserReader.toArray.length}; 接下来8个byte: ${loserReader.take(8).map(_.toHexString).toSeq}
        //             |
        //             |::
        //             |${BytesPrinter.toStr(arrayBuf)}
        //             |::::::::::::::::::::::::::::::::::::::
        //             |""".stripMargin)

      } else {
        loserReader.take(sysHeaderStartIndex) // 丢弃 ps header tag
        val pFrame = PESH264PFrame()
        loadFrameH264(loserReader, pFrame)
        frameDeque.add(pFrame)

        //        val toP = pFrame.getArray.map(p =>
        //          s""":::::::::::
        //             |${BytesPrinter.toStr(p)}
        //             |:::::::
        //             |""".stripMargin).reduce((a,b) => a + b)

        //        logger.info(
        //          s"""
        //             |一个p帧数据:
        //             |总数据包长度=$totalLength; 剩余reader长度=${loserReader.toArray.length}; 接下来8个byte: ${loserReader.take(8).toSeq}
        //             |::0000000000000000000000000000
        //             |$toP
        //             |::0000000000000000000000000000
        //             |""".stripMargin)
      }
    } else if (totalLength > 18 && isAudio) { // 音频
      //      logger.info(
      //        s"""
      //           |音频数据:
      //           |总数据包长度=$totalLength;
      //           |""".stripMargin)
      val aFrame = PSH264Audio()
      loadFrameH264(loserReader, aFrame)
      frameDeque.add(aFrame)
    } else { // 分包
      //      val tailPacket = loserReader.drop(14).toArray
      val tailPacket = loserReader.toArray
      if (frameDeque.size() > 0) {
        frameDeque.getLast match {
          case frame: PESFrame =>
            frame.addBytes(tailPacket)
          case _ =>
        }

        //        H264NALUFramer.nextUnit(tailPacket).foreach(ne => {
        //          logger.info(s"分包数据具有 nalu 分段 ${ne.nextStartTagLen}")
        //        })

        //        logger.info(
        //          s"""
        //             |分包数据: ${tailPacket.length}
        //             |总数据包长度=$totalLength;
        //             |""".stripMargin)
      } else {
        System.err.println(s"没有签字的分包")
      }
    }

    while (frameDeque.size() > 1) {
      onProduced(frameDeque.pop())
    }
    try ReferenceCountUtil.release(msg)
    catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

  private def loadFrameH264(reader: ByteLoserReader, frame: PESFrame): Unit = {

    var count = 1
    var pesStartIndex = reader.indexOfSlice(PSHeaders.PS_VIDEO_PES_HEADER)
    while (pesStartIndex >= 0) {
      reader.drop(pesStartIndex)
      val pts = getPts(reader)
      reader.drop(4)
      val pesPayloadLen = Buffers.wrap(reader.take(2).toArray).readUnsignedShort() // header tag后两个字节的长度，
      val pesHeaderLen = reader(2) & 0xFF
      reader.drop(3 + pesHeaderLen) // 丢弃pes头部所有
      val remainH264Length = pesPayloadLen - 3 - pesHeaderLen

      val naluArray = reader.take(remainH264Length).toArray
      frame.addBytes(naluArray)


      //      var naluArrayBuf = naluArray
      //      var maybeInfo = H264NALUFramer.nextUnit(naluArrayBuf)
      //
      //      var counrrr = 0
      //      while (maybeInfo.isDefined) {
      //        val setp = maybeInfo.get
      //        if (setp.nextStartTagLen == 3) {
      //
      //          System.err.println(s"第$counrrr ge nalu tag=${setp.nextStartTagLen}")
      //        }
      //        maybeInfo = H264NALUFramer.nextUnit(setp.leftover)
      //        //        naluArray.indexOfSlice(START_TAG4)
      //      }
      //
      //      val len = if (naluArray.indexOfSlice(START_TAG4) == 0) 4 else 3
      //      frame.addBytes(H264NaluData(len, naluArray.drop(len)))
      pesStartIndex = reader.indexOfSlice(PSHeaders.PS_VIDEO_PES_HEADER)

      if (pesStartIndex > 0) {
        System.err.println(s"一个包里面居然有被 忽略的数据超过${count += 1; count}个 pes 在 $pesStartIndex ; 此时包里还有size=${reader.size}")
      }
//      val i = reader.indexOfSlice(PSHeaders.PS_PES_HEADER)
//      if (i >= 0) System.err.println(s"下一个的 pes 标志在 $i , 包里还有size=${reader.size}")
    }
  }

  private def getPts(reader: ByteLoserReader): Long = {
    // 检查pes头中是否包含pts、dts ; i帧中的pes头至少需要携带pts
    val ptsDtsFlags: Byte = (reader(7) & 0xff >> 6 & 0x3).toByte

    //`11` pts、dts都有
    //`10` pts
    //00 都没有
    if (ptsDtsFlags == 0x3 || ptsDtsFlags == 0x2) {
      val bit32to30: Int = reader(10) & 0xff & 0xE
      val bit29to15: Int = Buffers.wrap(Array[Byte](reader(10), reader(11))).readUnsignedShort() & 0xFFFE
      val bit14to0: Int = Buffers.wrap(Array[Byte](reader(12), reader(13))).readUnsignedShort() & 0xFFFE
      (bit32to30 << 29).toLong + (bit29to15 << 14).toLong + (bit14to0 >> 1).toLong
    } else -1
  }
}
