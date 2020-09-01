package com.getouo.gb.scl.rtsp

import java.util.concurrent.Callable

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpRequest, FullHttpResponse, HttpResponseStatus}
import io.netty.handler.codec.rtsp.{RtspHeaderNames, RtspResponseStatuses, RtspVersions}

trait RtspExecuteable[V] extends Callable[V] {
  val channel: Channel
  val request: FullHttpRequest

  def baseResponse(status: HttpResponseStatus = RtspResponseStatuses.OK, payloadOpt: Option[ByteBuf]): FullHttpResponse = {
    val response = payloadOpt.map(payload => {
      val rep = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, status, payload)
      rep.headers().set(RtspHeaderNames.CONTENT_LENGTH, payload.readableBytes())
      rep.headers().add(RtspHeaderNames.CONTENT_BASE, request.uri())
      rep
    }).getOrElse(new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, status))
    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ))
    val session = request.headers.get(RtspHeaderNames.SESSION)
    if (session != null) response.headers.add(RtspHeaderNames.SESSION, session)
    response
  }

  def toResponse(status: HttpResponseStatus = RtspResponseStatuses.OK): FullHttpResponse =
    baseResponse(status, None)

  def payloadResponse(status: HttpResponseStatus = RtspResponseStatuses.OK, payload: ByteBuf): FullHttpResponse =
    baseResponse(status, Some(payload))
}
