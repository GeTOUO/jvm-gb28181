package com.getouo.gb.scl.rtsp

import io.netty.channel.Channel
import io.netty.handler.codec.http.{FullHttpRequest, FullHttpResponse}
import io.netty.handler.codec.rtsp.RtspHeaderNames

class OptionsExecutor(val channel: Channel, val request: FullHttpRequest) extends RtspExecuteable[FullHttpResponse] {
  override def call(): FullHttpResponse = {
    val response = toResponse()
    //    response.headers().add(RtspHeaderNames.PUBLIC, "DESCRIBE, SETUP, PLAY, TEARDOWN")
    response.headers().add(RtspHeaderNames.PUBLIC, "SETUP, PLAY, TEARDOWN")
    response
  }
}
