package com.getouo.gb.scl.io

import com.getouo.gb.scl.server.MediaStreamServer
import com.getouo.gb.scl.util.ChannelUtil
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil

class GB28181RealtimeSource() extends ChannelInboundHandlerAdapter with ActiveSource[GB28181SourceData] {
  val streamChannel = new MediaStreamServer(this)

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info(s"收到连接: ${ctx.channel().remoteAddress()}, " +
      s"remoteIp=${ChannelUtil.remoteIp(ctx.channel())}, remotePort=${ChannelUtil.castSocketAddr(ctx.channel().remoteAddress()).getPort}")
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    val byteBuf = msg.asInstanceOf[ByteBuf]
    val copyData = new Array[Byte](byteBuf.readableBytes)
    byteBuf.readBytes(copyData)

    onProduced(GB28181H264DataData(copyData))
    try ReferenceCountUtil.release(msg)
    catch {
      case e: Exception =>
        e.printStackTrace()
    }
//        logger.info(
//          s"""
//             |TcpMediaStreamHandler 收到数据， len=${msg.asInstanceOf[ByteBuf].readableBytes}
//             |""".stripMargin)
  }
}
