package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.util.LogSupport
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

class RtspTailHandler extends ChannelInboundHandlerAdapter with LogSupport {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info("channelActive: {}", ctx)
  }
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.info("exceptionCaught: {}", cause)
    ctx.close()
  }
  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = logger.info(s" 未捕获消息: ${msg.getClass}")
}
