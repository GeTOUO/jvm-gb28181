package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.util.LogSupport
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpRequest, HttpHeaderNames, HttpHeaders, HttpResponseStatus, HttpVersion}

class SipRequestHandler extends SimpleChannelInboundHandler[FullHttpRequest] with LogSupport {
  override def channelRead0(channelHandlerContext: ChannelHandlerContext, fullHttpRequest: FullHttpRequest): Unit = {

    val headers = fullHttpRequest.headers
    System.err.println(
      s"""
         |method=${fullHttpRequest.method()}  uri=${fullHttpRequest.uri()} version=${fullHttpRequest.protocolVersion()}
         |【Full】:
         |$fullHttpRequest
         |-------------->>>>>>>>>
         |""".stripMargin)

//    if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION)) && "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) handleHandshake(ctx, fullHttpRequest)
//    else sendHttpResponse(ctx, fullHttpRequest, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST))
  }
}
