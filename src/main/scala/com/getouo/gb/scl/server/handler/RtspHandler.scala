package com.getouo.gb.scl.server.handler

import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.{DefaultHttpRequest, HttpContent, HttpMethod}
import io.netty.util.internal.logging.{InternalLogger, InternalLoggerFactory}

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

        methodName.trim.toUpperCase match {
          case "OPTIONS" =>
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
        if (content.content().isReadable()) {
          /** 此时, 才表示HttpContent是有内容的, 否则,它是空的, 不需要处理 */
        }
      case _ =>
    }
  }
}
