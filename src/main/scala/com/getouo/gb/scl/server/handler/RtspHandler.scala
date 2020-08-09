package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.rtp.H264FileAccessor
import com.getouo.gb.util.MyTest
import io.netty.buffer.{ByteBuf, Unpooled, UnpooledByteBufAllocator}
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.HttpUtil.isKeepAlive
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil
import io.netty.util.internal.logging.{InternalLogger, InternalLoggerFactory}

class RtspHandler extends ChannelInboundHandlerAdapter {
  protected val logger: InternalLogger = InternalLoggerFactory.getInstance(this.getClass)

  override def channelActive(ctx: ChannelHandlerContext): Unit = logger.info("channelActive: {}", ctx)

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.info("exceptionCaught: {}", cause)
    ctx.close()
  }

  private var clientPort = 0
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
//        ctx.channel().re
        val CSeq: Int = req.headers().getInt("CSeq")
        methodName.trim.toUpperCase match {
          case "OPTIONS" =>
            val str = ResponseBuilder.buildOptions(CSeq, Seq("OPTIONS", "DESCRIBE", "SETUP", "TEARDOWN", "PLAY"))
//            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
            ctx.writeAndFlush(str)
          case "DESCRIBE" =>
            val str = ResponseBuilder.buildDescribe(CSeq)
            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
          case "SETUP" =>
            val transport = req.headers().get("Transport")
            val portStr = transport.split("client_port=")
            if (portStr.length == 2) {
              val ports = portStr(1).split("-")
              if (ports.length == 2) clientPort = ports(0).toInt
            }
            val str = ResponseBuilder.buildSetup(CSeq, transport, 56400, "66334873")
            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
          case "PLAY" =>
            val str = ResponseBuilder.buildPlay(CSeq, req.headers().get("Session"))
            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
            val accessor = new H264FileAccessor("E:/DevelopRepository/getouo/jvm-gb28181/src/main/resources/slamtv60.264")
//            Unpooled
//            new DatagramPacket(Unpooled.copiedBuffer(
//              "谚语查询结果："+nextQuote(),CharsetUtil.UTF_8), packet.sender())
            accessor.subscribe("c", clientPort)
            new Thread(accessor).start()
//            MyTest.mainStart(clientPort)
          case "TEARDOWN" =>
            val str = ResponseBuilder.buildTeardown(CSeq)
            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
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


  //1
  private def sendHttpResponse(ctx: ChannelHandlerContext, req: HttpRequest, res: DefaultFullHttpResponse): Unit = { // 返回应答给客户端
    if (res.status.code != 200) {
      val buf = Unpooled.copiedBuffer(res.status.toString, CharsetUtil.UTF_8)
      res.content.writeBytes(buf)
      buf.release
    }
    val f = ctx.channel.writeAndFlush(res)
    // 如果是非Keep-Alive，关闭连接
    if (!isKeepAlive(req) || res.status.code != 200) f.addListener(ChannelFutureListener.CLOSE)
  }
}

case class Content(uri: String, cseq: Int, content: String)


class ResponseBuilder(CSeq: Int) {
  var title: Option[String] = _
  val headers: collection.mutable.Map[String, Any] = new collection.mutable.HashMap[String, Any]()

}

object ResponseBuilder {

  def toByteBuf(resp: String): ByteBuf = {
    val bytes = resp.getBytes("UTF-8")
    val byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(bytes.length)
    byteBuf.writeBytes(bytes)
    byteBuf
  }

  def buildOptions(CSeq: Int, methods: Seq[String]): String = {
    val ms = if (methods.size < 1) methods.headOption.getOrElse("") else methods.reduce((a, b) => a + ", " + b)
    val s =
      s"""
         |RTSP/1.0 200 OK
         |CSeq: ${CSeq}
         |Public: ${ms}
         |
         |""".stripMargin
    s
  }

  def buildSetup(CSeq: Int, transport: String, serverPort: Int, session: String): String = {
    s"""
       |RTSP/1.0 200 OK
       |CSeq: ${CSeq}
       |Transport: ${transport};server_port=${serverPort}-${serverPort + 1}
       |Session: ${session}
       |
       |""".stripMargin
  }

  def buildDescribe(CSeq: Int): String = {
    s"""
       |RTSP/1.0 200 OK
       |CSeq: ${CSeq}
       |Content-length: 146
       |Content-type: application/sdp
       |
       |
       |v=0
       |o=- 91565340853 1 in IP4 192.168.31.115
       |t=0 0
       |a=contol:*
       |m=video 0 RTP/AVP 96
       |a=rtpmap:96 H264/90000
       |a=framerate:25
       |a=control:track0
       |""".stripMargin
  }

  def buildPlay(CSeq: Int, session: String): String = {
    s"""
       |RTSP/1.0 200 OK
       |CSeq: ${CSeq}
       |Range: npt=0.000-
       |Session: ${session}; timeout=60
       |
       |""".stripMargin
  }

  def buildTeardown(CSeq: Int): String = {
    s"""
       |RTSP/1.0 200 OK
       |CSeq: ${CSeq}
       |
       |""".stripMargin
  }
}