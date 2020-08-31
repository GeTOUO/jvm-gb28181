package com.getouo.gb.scl.rtsp

import java.util.concurrent.Callable

import io.netty.channel.Channel
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpRequest, FullHttpResponse, HttpResponseStatus}
import io.netty.handler.codec.rtsp.{RtspHeaderNames, RtspResponseStatuses, RtspVersions}

trait RtspExecuteable[V] extends Callable[V] {
  val channel: Channel
  val request: FullHttpRequest

  def toResponse(status: HttpResponseStatus = RtspResponseStatuses.OK): FullHttpResponse = {
    val response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, status)
    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ))
    response
  }
}
