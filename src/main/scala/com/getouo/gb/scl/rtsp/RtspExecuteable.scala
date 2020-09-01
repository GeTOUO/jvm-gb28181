package com.getouo.gb.scl.rtsp

import java.util.concurrent.Callable

import io.netty.buffer.ByteBuf
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

  def payloadResponse(status: HttpResponseStatus = RtspResponseStatuses.OK, payload: ByteBuf): FullHttpResponse = {
    val response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, status, payload)
    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ))
    response.headers().set(RtspHeaderNames.CONTENT_LENGTH, payload.readableBytes())
    response.headers().add(RtspHeaderNames.CONTENT_BASE, request.uri())
    response
  }
}
