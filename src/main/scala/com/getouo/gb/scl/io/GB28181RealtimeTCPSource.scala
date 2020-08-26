package com.getouo.gb.scl.io

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

import com.getouo.gb.scl.data.{PSH264Data, PSH264IFrame, PSH264PFrame}
import com.getouo.gb.scl.rtp.RtpHeader
import com.getouo.gb.scl.server.RealtimeMediaStreamServer
import com.getouo.gb.scl.util.{ByteLoserReader, ChannelUtil}
import com.getouo.gb.util.BytesPrinter
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil
import io.pkts.buffer.Buffers

class GB28181RealtimeTCPSource() extends ChannelInboundHandlerAdapter with ActiveSource[PSH264Data] {
  val streamChannel = new RealtimeMediaStreamServer(this)
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

    val hasPSHeader = loserReader.matcher(0, PSH264Data.PS_HEADER)
    val isAudio = loserReader.matcher(0, PSH264Data.PS_AUDIO_PES_HEADER)

    if (totalLength > 18 && hasPSHeader) { // 有ps头,进一步判断是否是i帧或者p帧
      //      val psHeader = loserReader.take(14)
      //      val stuffingLen = psHeader.last & 0x07
      //      val stuffingLength: Int = loserReader(13) & 0x07
      //      val startIndex = 14 + stuffingLength

      // ps 头前14字节是固定的， [第14字节 & 0x07] = 得到扩展的长度
      val sysHeaderStartIndex = 14 + (loserReader(13) & 0x07)
      val isIFrame = loserReader.length > 13 && loserReader.matcher(sysHeaderStartIndex, PSH264Data.PS_SYSTEM_HEADER_I_FRAME)

      if (isIFrame) {
        loserReader.drop(sysHeaderStartIndex + 4) // 丢弃 system header tag及之前的数据
        val sysHeaderLen = Buffers.wrap(loserReader.take(2).toArray).readUnsignedShort() // header tag后两个字节的长度，
        loserReader.drop(sysHeaderLen) // 丢弃 system头数据及之前的数据

        loserReader.drop(4) // 丢弃 system map tag
        val sysMapLen = Buffers.wrap(loserReader.take(2).toArray).readUnsignedShort() // header tag后两个字节的长度，
        loserReader.drop(sysMapLen) // 丢弃 system map 数据

        // pes 开始:
        val pts = getPts(loserReader)
        val iFrame = PSH264IFrame(pts)
        loadIFrameH264(loserReader, iFrame)
        frameDeque.add(iFrame)

        logger.info(
          s"""
             |一个iiiiiiiiiiiiiiiiiii帧数据:----------------------------------
             |总数据包长度=$totalLength; 剩余reader长度=${loserReader.toArray.length}; 接下来8个byte: ${loserReader.take(8).map(_.toHexString).toSeq}
             |
             |::
             |${BytesPrinter.toStr(arrayBuf)}
             |::::::::::::::::::::::::::::::::::::::
             |""".stripMargin)

      } else {
        loserReader.take(sysHeaderStartIndex) // 丢弃 ps header tag
        val pFrame = PSH264PFrame(0)
        loadPFrameH264(loserReader, pFrame)
        frameDeque.add(pFrame)

        logger.info(
          s"""
             |一个p帧数据:
             |总数据包长度=$totalLength; 剩余reader长度=${loserReader.toArray.length}; 接下来8个byte: ${loserReader.take(8).toSeq}
             |""".stripMargin)
      }

    } else if (totalLength > 18 && isAudio) { // 音频
      logger.info(
        s"""
           |音频数据:
           |总数据包长度=$totalLength;
           |""".stripMargin)
    } else { // 分包
      if (frameDeque.size() > 0) {
        val last = frameDeque.getLast
        last match {
          case frame: PSH264IFrame =>
            frame.addBytes(loserReader.drop(14).toArray)
          case _ =>
        }
      }
      logger.info(
        s"""
           |分包数据:
           |总数据包长度=$totalLength;
           |""".stripMargin)
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

  private def loadPFrameH264(reader: ByteLoserReader, frame: PSH264PFrame): Unit = {
    while (reader.indexOfSlice(PSH264Data.PS_VIDEO_PES_HEADER) == 0) {
      reader.drop(4)
      val pesPayloadLen = Buffers.wrap(reader.take(2).toArray).readUnsignedShort() // header tag后两个字节的长度，
      val pesHeaderLen = reader(2) & 0xFF
      reader.drop(3 + pesHeaderLen) // 丢弃pes头部所有
      val remainH264Length = pesPayloadLen - 3 - pesHeaderLen
      frame.addBytes(reader.take(remainH264Length).toArray)
    }
  }

  private def loadIFrameH264(reader: ByteLoserReader, frame: PSH264IFrame): Unit = {
    while (reader.indexOfSlice(PSH264Data.PS_VIDEO_PES_HEADER) == 0) {
      reader.drop(4)
      val pesPayloadLen = Buffers.wrap(reader.take(2).toArray).readUnsignedShort() // header tag后两个字节的长度，
      val pesHeaderLen = reader(2) & 0xFF
      reader.drop(3 + pesHeaderLen) // 丢弃pes头部所有
      val remainH264Length = pesPayloadLen - 3 - pesHeaderLen
      frame.addBytes(reader.take(remainH264Length).toArray)
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
