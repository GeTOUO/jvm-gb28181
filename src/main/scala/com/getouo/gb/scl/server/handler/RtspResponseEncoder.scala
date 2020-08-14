package com.getouo.gb.scl.server.handler

import java.util

import com.getouo.gb.scl.model.RtspResponse
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

class RtspResponseEncoder extends MessageToMessageEncoder[RtspResponse] {
  override def encode(channelHandlerContext: ChannelHandlerContext, i: RtspResponse, list: util.List[AnyRef]): Unit =
    list.add(i.stringMessage())
}
