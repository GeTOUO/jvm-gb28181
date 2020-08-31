package com.getouo.gb.scl.rtsp

import io.netty.channel.Channel
import io.netty.handler.codec.http.{FullHttpRequest, FullHttpResponse}

class DescribeExecutor(val channel: Channel, val request: FullHttpRequest) extends RtspExecuteable[FullHttpResponse] {
  override def call(): FullHttpResponse = {
    val response = toResponse()
    response
  }
}
