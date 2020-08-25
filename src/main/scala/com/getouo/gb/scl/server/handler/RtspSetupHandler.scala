package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.data.ISourceData
import com.getouo.gb.scl.io.{H264FileSource, Source}
import com.getouo.gb.scl.model.{RtspSetupRequest, RtspSetupResponse}
import com.getouo.gb.scl.rtp.H264RtpConsumer
import com.getouo.gb.scl.server.{GBStreamPublisher, UdpPusher}
import com.getouo.gb.scl.service.DeviceService
import com.getouo.gb.scl.stream.{FileSourceId, GB28181PlayStream, GBSourceId, H264ConsumptionPipeline, H264PlayStream, PlayStream, SourceConsumer, SourceId}
import com.getouo.gb.scl.util._
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}

class RtspSetupHandler extends SimpleChannelInboundHandler[RtspSetupRequest] with LogSupport {

  private def sourceSetup[ID <: SourceId, S <: Source[IN], IN <: ISourceData, OUT <: ISourceData, SC <: SourceConsumer[_]]
  (channel: Channel, i: RtspSetupRequest, rtpSession: RtpSession, ps: PlayStream[ID, S, IN, OUT], consumer: SC): RtspSetupResponse = {
    i.rtpTransType match {
      case ConstVal.RtpOverUDP(sIp, targetIp, targetPort, castType) =>
        val udpChannel = SpringContextUtil.getBean(clazz = classOf[UdpPusher]).getOrElse(throw new Exception(s"获取UdpPusher失败")).channel
        consumer.udpJoin(udpChannel, (targetIp, targetPort))
      case ConstVal.RtpOverTCP(tv) =>
        consumer.tcpJoin(channel)
      case ConstVal.CustomRtpTransType(value, v2transport) =>
        logger.info(s"未处理扩展传输方式: $value")
      case ConstVal.UnknownTransType =>
        logger.info(s"UnknownTransType")
      case _ =>
        logger.info(s"未知传输方式")
    }
    val toUpdate = rtpSession.copy(pt = i.rtpTransType, playStreamOpt = Some(ps), consumerOpt = Some(consumer))
    Session.updateChannelSession(channel, _ => Some(toUpdate))
    val response = RtspSetupResponse(i.CSeq, toUpdate.pt, toUpdate.idHash())
    logger.info(
      s"""
         |SETUP RESP:
         |$response
         |""".stripMargin)
    response
  }

  override def channelRead0(ctx: ChannelHandlerContext, i: RtspSetupRequest): Unit = {
    val channel = ctx.channel()
    val sessionOpt = Session.rtpSession(channel)
    logger.info(s"准备资源: ${sessionOpt} 的 setup")
    sessionOpt match {
      case None => logger.error(s"session 未创建 on client_port=${ChannelUtil.castSocketAddr(ctx.channel().remoteAddress()).getPort}")
      case Some(rtpSession) =>
        rtpSession.id match {
          case fid@FileSourceId(file, setupTime) =>
            val tuple = fileSourceConsumer(channel, fid)
            val resp = sourceSetup(channel, i, rtpSession, tuple._1, tuple._2)
            ctx.writeAndFlush(resp)
          case gid@GBSourceId(deviceId, channelId, setupTime) =>
            val tuple = gbSourceConsumer(channel, gid)
            val resp = sourceSetup(channel, i, rtpSession, tuple._1, tuple._2)
            ctx.writeAndFlush(resp)
          case _ =>
            logger.info(s"未知的id类型 ${rtpSession.id.getClass}")
        }
    }
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  private def fileSourceConsumer(channel: Channel, fid: FileSourceId): (H264PlayStream, H264RtpConsumer) = {
    val stream = H264PlayStream.getOrElseSubmit(fid, id => new H264PlayStream(id, new H264FileSource(id.file), new H264ConsumptionPipeline))
    (stream, stream.getOrElseAddConsumer(classOf[H264RtpConsumer], new H264RtpConsumer()))
  }

  private def gbSourceConsumer(channel: Channel, gid: GBSourceId): (GB28181PlayStream, GBStreamPublisher) = {
    val deviceService = SpringContextUtil.getBean(clazz = classOf[DeviceService]).getOrElse(throw new Exception(s"无法取得 deviceService"))
    val ps: GB28181PlayStream = deviceService.getPlayStream(gid)
    (ps, deviceService.getGBPublisher(ps))
  }

}