package com.getouo.gb.scl.io

import java.util.concurrent.atomic.AtomicReference

import com.getouo.gb.scl.data.{PSH264Data, PSH264IFrame}
import com.getouo.gb.scl.rtp.RtpHeader
import com.getouo.gb.scl.server.RealtimeMediaStreamServer
import com.getouo.gb.scl.util.{ByteLoserReader, ChannelUtil}
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

  /**
   * 收到经过 LengthFieldBasedFrameDecoder 处理的数据。
   *
   * @param ctx
   * @param msg
   */
  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    val loserReader = toReader(msg.asInstanceOf[ByteBuf])
    val totalLength = loserReader.size
    if (totalLength <= 14) return // 国标的rtp over tcp 具有2个字节长度头和12字节的rtp头。总长度小于这个值表面无有效数据
    val len = Buffers.wrap(loserReader.take(2).toArray).readUnsignedShort()
    val rtpHeader = RtpHeader(loserReader.take(12).toArray)
    val sequenceNumber = rtpHeader.sequenceNumber

    val hasPSHeader = loserReader.matcher(0, Array(0.toByte, 0.toByte, 1.toByte, 0xba.toByte))
    val isAudio = loserReader.matcher(0, Array(0.toByte, 0.toByte, 1.toByte, 0xc0.toByte))

    if (totalLength > 18 && hasPSHeader) { // 有ps头,进一步判断是否是i帧或者p帧
      val stuffingLength: Int = loserReader(13) & 7
      val startIndex = 13 + stuffingLength + 1
      val isIFrame = loserReader.matcher(startIndex, Array(0.toByte, 0.toByte, 1.toByte, 0xbb.toByte))

      if (isIFrame) {

      } else {

      }


    } else if (totalLength > 18 && isAudio) { // 音频

    } else { // 分包

    }


    onProduced(PSH264IFrame(loserReader.toArray))
    try ReferenceCountUtil.release(msg)
    catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }
}
