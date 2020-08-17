package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.io.H264FileSource
import com.getouo.gb.scl.model.{RtspSetupRequest, RtspSetupResponse}
import com.getouo.gb.scl.rtp.H264RtpConsumer
import com.getouo.gb.scl.stream.{FileSourceId, H264ConsumptionPipeline, H264PlayStream}
import com.getouo.gb.scl.util._
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}

class RtspSetupHandler extends SimpleChannelInboundHandler[RtspSetupRequest] with LogSupport {
  override def channelRead0(ctx: ChannelHandlerContext, i: RtspSetupRequest): Unit = {
    logger.info(s"准备资源: ${i.url} 的 setup")

    Session.rtpSession(ctx.channel()) match {
      case None => logger.error(s"session 未创建 on client_port=${ChannelUtil.castSocketAddr(ctx.channel().remoteAddress()).getPort}")
      case Some(session) =>
        i.rtpTransType match {
          case udp@ConstVal.RtpOverUDP(_, targetIp, targetPort, castType) =>
            val response = udpSetup(session.id, i.CSeq, udp, ctx.channel())
            logger.error(s" setup 即将回复: ${response.stringMessage()}")
            ctx.writeAndFlush(response)
          case tcp@ConstVal.RtpOverTCP() =>
            logger.info("暂未支持tcp传输")
          case ConstVal.CustomRtpTransType(value, v2transport) => println(s"未处理扩展传输方式: $value")
          case ConstVal.UnknownTransType => println("位置传输方式")
        }
    }
  }

  private def udpSetup(sourceName: String, CSeq: Int, transType: ConstVal.RtpOverUDP, channel: Channel): RtspSetupResponse = {
    Session.updateChannelSession(channel, opt => {
      val stream = H264PlayStream.getOrElseUpdateLocalFileH264Stream(FileSourceId(sourceName, System.currentTimeMillis()), id => {
        import scala.concurrent.ExecutionContext.Implicits.global
        new H264PlayStream(id, new H264FileSource(sourceName), new H264ConsumptionPipeline)
      })
      val consumer = stream.getConsumerOrElseUpdate(classOf[H264RtpConsumer], new H264RtpConsumer())
//      while (RequestHandler.server.rtpUDPServer.channel == null) {
//        Thread.sleep(1)
//      }
//      transType.updateServerPort(ChannelUtil.castSocketAddr(RequestHandler.server.rtpUDPServer.channel.localAddress()).getPort)
      consumer.udpJoin(RequestHandler.server.rtpUDPServer.channel, (transType.targetIp, transType.targetPort))
      Some(opt match {
        case Some(session: RtpSession) =>
          session.copy(pt = transType, playStreamOpt = Some(stream), consumerOpt = Some(consumer))
        case _ =>
          RtpSession(sourceName, transType, Some(stream), Some(consumer))
      })
    })
    val session = Session.rtpSession(channel).get
    RtspSetupResponse(CSeq, session.pt, session.idHash())
  }
}