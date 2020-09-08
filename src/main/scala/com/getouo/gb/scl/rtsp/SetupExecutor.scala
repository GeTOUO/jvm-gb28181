package com.getouo.gb.scl.rtsp

import com.getouo.gb.scl.io.H264FileSource
import com.getouo.gb.scl.model.GBDevice
import com.getouo.gb.scl.rtp.H264RtpConsumer
import com.getouo.gb.scl.server.UdpPusher
import com.getouo.gb.scl.service.DeviceService
import com.getouo.gb.scl.stream._
import com.getouo.gb.scl.util.ConstVal.{RtpTransport, RtpTransportOverUDP}
import com.getouo.gb.scl.util._
import io.netty.channel.Channel
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpRequest, FullHttpResponse}
import io.netty.handler.codec.rtsp.{RtspHeaderNames, RtspResponseStatuses, RtspVersions}

class SetupExecutor(val channel: Channel, val request: FullHttpRequest) extends RtspExecuteable[FullHttpResponse] with LogSupport {
  override def call(): FullHttpResponse = {

    val serverIp = NetAddressUtil.localAddress.getHostAddress
    val transport = request.headers().get(RtspHeaderNames.TRANSPORT)
    val rtpTransport = RtpTransport.valueOf(transport)

    val sessionOpt = Session.rtpSession(channel).map(_.copy(pt = rtpTransport))

    sessionOpt.map(loadSource) match {
      case None =>
        logger.error(s"session 未创建 on client_port=${ChannelUtil.castSocketAddr(channel.remoteAddress()).getPort}")
        new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.SESSION_NOT_FOUND)
      case Some(rtpSession) =>
        val pt = rtpSession.pt
        rtpSession.consumerOpt match {
          case None => return new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.UNSUPPORTED_MEDIA_TYPE)
          case Some(consumer) =>
            if (pt.isUdp) {
              consumer.udpJoin(SpringContextUtil.getBean(clazz = classOf[UdpPusher]).getOrElse(throw new Exception(s"获取UdpPusher失败")).channel,
                (ChannelUtil.remoteIp(channel), pt.asInstanceOf[RtpTransportOverUDP].clientPortL))
            } else if (pt.isTcp) {
              consumer.tcpJoin(channel)
            } else {
              return new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.UNSUPPORTED_TRANSPORT)
            }
        }
        Session.updateChannelSession(channel, _ => Some(rtpSession))
        val response = toResponse()
        response.headers().set(RtspHeaderNames.TRANSPORT, pt.transportValue())
        response.headers().set(RtspHeaderNames.SESSION, rtpSession.idHash())
        response
    }
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def loadSource(rtpSession: RtpSession): RtpSession = {
    rtpSession.id match {
      case fid@FileSourceId(file, setupTime) =>
        val stream = H264PlayStream.getOrElseSubmit(fid, id => new H264PlayStream(id, new H264FileSource(id.file), new H264ConsumptionPipeline))
        val consumer = stream.getOrElseAddConsumer(classOf[H264RtpConsumer], new H264RtpConsumer())
        rtpSession.copy(playStreamOpt = Some(stream), consumerOpt = Some(consumer))
      case gid@GBSourceId(deviceId, channelId, setupTime) =>
        val deviceService = SpringContextUtil.getBean(clazz = classOf[DeviceService]).getOrElse(throw new Exception(s"无法取得 deviceService"))
        val ps = GBDevice.getPlayStream(gid)
        val publisher = GBDevice.getGBPublisher(ps)
        rtpSession.copy(playStreamOpt = Some(ps), consumerOpt = Some(publisher))
      case _ => rtpSession
    }
  }
}
