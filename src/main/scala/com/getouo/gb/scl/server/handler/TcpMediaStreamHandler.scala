package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.util.{ChannelUtil, LogSupport}
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

class TcpMediaStreamHandler extends ChannelInboundHandlerAdapter with LogSupport {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ChannelUtil.remoteIp(ctx.channel())
    logger.info(s"收到连接: ${ctx.channel().remoteAddress()}, " +
      s"remoteIp=${ChannelUtil.remoteIp(ctx.channel())}, remotePort=${ChannelUtil.castSocketAddr(ctx.channel().remoteAddress()).getPort}")
  }


  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {

//    logger.info(
//      s"""
//         |TcpMediaStreamHandler 收到数据， len=${msg.asInstanceOf[ByteBuf].readableBytes}
//         |""".stripMargin)
  }
}
