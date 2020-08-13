package com.getouo.gb.scl.server.handler

import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

import com.getouo.gb.scl
import com.getouo.gb.scl.io.H264FileSource
import com.getouo.gb.scl.rtp.{H264RtpConsumer, SDPInfoBuilder}
import com.getouo.gb.scl.server.RtpAndRtcpServerGroup
import com.getouo.gb.scl.stream.{FileSourceId, H264ConsumptionPipeline, H264PlayStream, PlayStream}
import io.netty.buffer.{ByteBuf, Unpooled, UnpooledByteBufAllocator}
import io.netty.channel.{Channel, ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.HttpUtil.isKeepAlive
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil
import io.netty.util.internal.logging.{InternalLogger, InternalLoggerFactory}

class RtspHandler extends ChannelInboundHandlerAdapter {
  protected val logger: InternalLogger = InternalLoggerFactory.getInstance(this.getClass)

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info("channelActive: {}", ctx)
//    if (!ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress].getAddress.getHostAddress.contains("192.168.2.19")) {
//      ResponseBuilder.accessor.subscribeTCP(ctx.channel())
//    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.info("exceptionCaught: {}", cause)
    ctx.close()
  }

//  private var clientPort = 0
  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    logger.info("[RTSPHandler]channelRead:  {}", msg.getClass)
    msg match {
      case req: DefaultHttpRequest =>
        RequestHandler.dispatcherRequest(ctx.channel(), req)
//
//        val method: HttpMethod = req.method()
//        val methodName: String = method.name()
//        logger.info("method: {}", methodName)
//        logger.info("req: {}", req)
//        logger.info("req.ur: {}", req.uri())
//        logger.info("req.headers: {}", req.headers())
////        ctx.channel().re
//        val CSeq: Int = req.headers().getInt("CSeq")
//        methodName.trim.toUpperCase match {
//          case "OPTIONS" =>
//            val str = ResponseBuilder.buildOptionsResp(CSeq, Seq("OPTIONS", "DESCRIBE", "SETUP", "TEARDOWN", "PLAY"))
////            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
//            ctx.writeAndFlush(str)
//          case "DESCRIBE" =>
//            val str = ResponseBuilder.buildDescribeResp(CSeq)
//            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
//          case "SETUP" =>
//            val transport = req.headers().get("Transport")
//            val portStr = transport.split("client_port=")
//            if (portStr.length == 2) {
//              val ports = portStr(1).split("-")
//              if (ports.length == 2) clientPort = ports(0).toInt
//            }
//            val str = ResponseBuilder.buildSetupResp(CSeq, transport, 56400, "66334873")
//            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
//          case "PLAY" =>
//            val str = ResponseBuilder.buildPlayResp(CSeq, req.headers().get("Session"))
//            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
////            val accessor = new H264FileAccessor("E:/DevelopRepository/getouo/jvm-gb28181/src/main/resources/slamtv60.264")
////            val accessor = new H264FileAccessor("F:\\h264file/nature_704x576_25Hz_1500kbits.h264")
////            val accessor = new H264FileAccessor("F:\\h264file/src13_hrc7_525_420_2.264")
////            val accessor = new H264FileAccessor("G:/slamtv60.264")
////            Unpooled
////            new DatagramPacket(Unpooled.copiedBuffer(
////              "谚语查询结果："+nextQuote(),CharsetUtil.UTF_8), packet.sender())
////            val accessor = new H264FileAccessor("/slamtv60.264")
////            accessor.subscribe("c", clientPort)
////            new Thread(accessor).start()
////            MyTest.mainStart(clientPort, "F:\\h264file/nature_704x576_25Hz_1500kbits.h264")
//          case "TEARDOWN" =>
//            val str = ResponseBuilder.buildTeardown(CSeq)
//            ctx.writeAndFlush(ResponseBuilder.toByteBuf(str))
//          case _ =>
//        }

      case content: HttpContent =>
        logger.warn("HttpContent=" + content)
        if (content.content().isReadable()) {
          /** 此时, 才表示HttpContent是有内容的, 否则,它是空的, 不需要处理 */
          logger.error(content.content().toString(Charset.defaultCharset()))

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


class RequestHandler(CSeq: Int) {
  var title: Option[String] = _
  val headers: collection.mutable.Map[String, Any] = new collection.mutable.HashMap[String, Any]()

}

object RequestHandler {
  val isStart = new AtomicBoolean(false)

  protected val logger: InternalLogger = InternalLoggerFactory.getInstance(this.getClass)
//  val accessor = new H264FileAccessor("src/main/resources/slamtv60.264")

  var clientPort: Int = 0
  val server = new RtpAndRtcpServerGroup(23456)
  def toByteBuf(resp: String): ByteBuf = {
    val bytes = resp.getBytes("UTF-8")
    val byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(bytes.length)
    byteBuf.writeBytes(bytes)
    byteBuf
  }

  def dispatcherRequest(channel: Channel, req: DefaultHttpRequest): Unit = {
    req.method().name().trim.toUpperCase match {
      case "OPTIONS" => execOptions(channel, req)
      case "DESCRIBE" => execDescribe(channel, req)
      case "SETUP" => execSetup(channel, req)
      case "PLAY" => execPlay(channel, req)
      case "TEARDOWN" => channel.writeAndFlush(buildTeardownResp(req.headers().getInt("CSeq")))
    }
  }

  def execOptions(channel: Channel, req: DefaultHttpRequest): Unit = {
    val reqSeq = req.headers().getInt("CSeq")
    logger.info(s"request options: CSeq=${reqSeq}, request=${req}")
    channel.writeAndFlush(buildOptionsResp(reqSeq))
  }

  def execDescribe(channel: Channel, req: DefaultHttpRequest): Unit = {
    val reqSeq = req.headers().getInt("CSeq")
    logger.info(s"request describe: CSeq=${reqSeq}, request=${req}")
    val str = buildDescribeResp(reqSeq, channel.localAddress().asInstanceOf[InetSocketAddress].getAddress.getHostAddress)
    logger.info(s"response describe: CSeq=${reqSeq}, response=${str}")
    channel.writeAndFlush(str)
  }

  def execSetup(channel: Channel, req: DefaultHttpRequest): Unit = {
    val reqSeq = req.headers().getInt("CSeq")

    val transport = req.headers().get("Transport")
    clientPort = scl.extractClientTransport(transport)
    logger.info(s"request setup: CSeq=${reqSeq}, request=$req")
    channel.writeAndFlush(buildSetupResp(reqSeq, transport, server.rtpPort, "66334873"))
//    channel.writeAndFlush(buildSetupResp(reqSeq, transport, 56400, "66334873"))
  }

  def execPlay(channel: Channel, req: DefaultHttpRequest): Unit = {

//    accessor.subscribe("c", clientPort)
//    if (isStart.compareAndSet(false, true)) {
//      new Thread(RequestHandler.accessor).start()
//    }
    logger.info(s"request play: CSeq=${req.headers().getInt("CSeq")}, request=${req}")
    channel.writeAndFlush(buildPlayResp(req.headers().getInt("CSeq"), req.headers().get("Session")))

    import scala.concurrent.ExecutionContext.Implicits.global

    val ps: H264PlayStream = H264PlayStream.getOrElseUpdateLocalFileH264Stream(FileSourceId("src/main/resources/slamtv60.264", System.currentTimeMillis()), id => {
      new H264PlayStream(id, new H264FileSource(id.file), new H264ConsumptionPipeline())
    })
    val consumer = ps.getConsumerOrElseUpdate(classOf[H264RtpConsumer], new H264RtpConsumer())
    consumer.udpJoin(RequestHandler.server.rtpUDPServer.channel, ("192.168.2.19", clientPort))
    ps.submit()
  }

  def buildOptionsResp(CSeq: Int, methods: Seq[String] = Seq("OPTIONS", "DESCRIBE", "SETUP", "TEARDOWN", "PLAY")): String = {
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

  def buildDescribeResp(CSeq: Int, localIp: String): String = {
    val body = SDPInfoBuilder.build(localIp)
    s"""
       |RTSP/1.0 200 OK
       |CSeq: ${CSeq}
       |Content-length: ${body.length}
       |Content-type: application/sdp
       |
       |${body}
       |""".stripMargin
  }


  def buildSetupResp(CSeq: Int, transport: String, serverPort: Int, session: String): String = {
    s"""
       |RTSP/1.0 200 OK
       |CSeq: ${CSeq}
       |Transport: ${transport};server_port=${serverPort}-${serverPort + 1}
       |Session: ${session}
       |
       |""".stripMargin
  }

  def buildPlayResp(CSeq: Int, session: String): String = {
    s"""
       |RTSP/1.0 200 OK
       |CSeq: ${CSeq}
       |Range: npt=0.000-
       |Session: ${session}; timeout=60
       |
       |""".stripMargin
  }

  def buildTeardownResp(CSeq: Int): String = {
    s"""
       |RTSP/1.0 200 OK
       |CSeq: ${CSeq}
       |
       |""".stripMargin
  }
}