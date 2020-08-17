package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.model.RtspPlayRequest
import com.getouo.gb.scl.util._
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

class RtspPlayHandler extends SimpleChannelInboundHandler[RtspPlayRequest] with LogSupport {
  override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: RtspPlayRequest): Unit = {
    logger.info(s"准备播放: ${i.url}")

    Session.rtpSession(channelHandlerContext.channel()) match {
      case Some(session) =>
        session.playStreamOpt match {
          case Some(value) =>
            value.submit()
            channelHandlerContext.writeAndFlush(i.defaultResponse())
          case None => logger.warn(s"未经过 setup, playStream 不存在")
        }
      case None => logger.warn(s"session 不存在")
    }
  }
}