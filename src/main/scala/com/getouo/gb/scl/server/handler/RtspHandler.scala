package com.getouo.gb.scl.server.handler

import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.{DefaultHttpRequest, HttpContent, HttpHeaderValues, HttpMethod}
import io.netty.util.internal.logging.{InternalLogger, InternalLoggerFactory}

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.util.CharsetUtil

class RtspHandler extends ChannelInboundHandlerAdapter {
  protected val logger: InternalLogger = InternalLoggerFactory.getInstance(this.getClass)

  override def channelActive(ctx: ChannelHandlerContext): Unit = logger.info("channelActive: {}", ctx)

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.info("exceptionCaught: {}", cause)
    ctx.close()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    logger.info("[RTSPHandler]channelRead:  {}", msg.getClass)
    msg match {
      case req: DefaultHttpRequest =>
        val method: HttpMethod = req.method()
        val methodName: String = method.name()
        logger.info("method: {}", methodName)
        logger.info("req: {}", req)
        logger.info("req.ur: {}", req.uri())
        logger.info("req.headers: {}", req.headers())

        val CSeq: Int = req.headers().getInt("CSeq")
        logger.info("req.headers.CSeq: {}", CSeq)

        methodName.trim.toUpperCase match {
          case "OPTIONS" => response(ctx, )
          case "DESCRIBE" =>
          case "SETUP" =>
          case "PLAY" =>
          case "TEARDOWN" =>
          case _ =>
        }

        /** 以下就是具体消息的处理, 需要开发者自己实现 */
        if (methodName.equalsIgnoreCase("OPTIONS") ||
          methodName.equalsIgnoreCase("DESCRIBE")) {
        } else {

        }
      case content: HttpContent =>
        logger.warn("HttpContent=" + content)
        if (content.content().isReadable()) {
          /** 此时, 才表示HttpContent是有内容的, 否则,它是空的, 不需要处理 */
        }
      case _ =>
    }
  }


  // https://blog.csdn.net/qq_33210338/article/details/105083247
  private def response(ctx: ChannelHandlerContext, c: Content): Unit = { // 1.设置响应
    Unpooled.EMPTY_BUFFER
    val resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER)
    resp.headers.set("CSeq", c.cseq)
    // 2.发送
    // 注意必须在使用完之后，close channel
    ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE)
  }
}

case class Content(uri: String, cseq: Int, content: String)

class ResponseContentBuilder(CSeq: Int) {
  var title: Option[String] = _
  val headers: collection.mutable.Map[String, Any] = new collection.mutable.HashMap[String, Any]()

}