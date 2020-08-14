package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.model.RtspOptionsRequest
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

class RtspOptionsHandler extends SimpleChannelInboundHandler[RtspOptionsRequest] {
  override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: RtspOptionsRequest): Unit = {
    i.defaultResponse()
  }
}
