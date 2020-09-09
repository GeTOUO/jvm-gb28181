package com.getouo.gb.scl.model

import java.net.InetSocketAddress

import com.getouo.gb.scl.io.GB28181RealtimeTCPSource
import com.getouo.gb.scl.server.GBStreamPublisher
import com.getouo.gb.scl.sip.ChannelGroups
import com.getouo.gb.scl.stream.{GB28181ConsumptionPipeline, GB28181PlayStream, GBSourceId}
import com.getouo.gb.scl.util.{ChannelUtil, NetAddressUtil}
import com.getouo.sip._
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{Channel, ChannelId}
import io.netty.util.CharsetUtil
import io.netty.util.concurrent.Future

import scala.util.{Failure, Success, Try}

case class SipConnection(isUdp: Boolean, channelId: ChannelId)

case class GBDevice(id: String, recipientAddress: InetSocketAddress, connection: SipConnection) {

  val ip: String = recipientAddress.getAddress.getHostAddress
  val port: Int = recipientAddress.getPort

  def channelOpt: Option[Channel] = if (connection.isUdp) {
    ChannelGroups.find(connection.channelId, ChannelGroups.SIP_UDP_POINT)
  } else ChannelGroups.find(connection.channelId)

  def play(serverId: String): String = {
    channelOpt match {
      case None => "设备未连接"
      case Some(channel) =>
        Try {

          val localIp = NetAddressUtil.localAddress.getHostAddress
          val sourceId = GBSourceId(id, id)
          val ps: GB28181PlayStream = GBDevice.getPlayStream(sourceId)
          val consumer: GBStreamPublisher = GBDevice.getGBPublisher(ps)
          val localPort = ChannelUtil.castSocketAddr(channel.localAddress()).getPort
          val playMessage = this.inviteMessage(localIp, localPort, ps.source.streamChannel.localPort, serverId, connection.isUdp)

          System.err.println(
            s"""
               |-------playMessage
               |$playMessage""".stripMargin)
          channel.writeAndFlush(playMessage)
          s"rtsp://$localIp:${consumer.localPort}"
        } match {
          case Failure(exception) =>exception.printStackTrace(); exception.getMessage
          case Success(value) =>value
        }
    }
  }

  def keepalive(req: FullSipRequest): Unit = {
    val response = req.createResponse(SipResponseStatus.OK)
    channelOpt.foreach(channel => channel.writeAndFlush(response))

  }

  def inviteMessage(serverIp: String, serverPort: Int, serverMediaPort: Int, serverId: String, sipIsUdp: Boolean): FullSipRequest = {
    val tcpSsrc = s"0${serverId.substring(3, 8)}0000"
    // m=video $serverMediaPort TCP/RTP/AVP 96 98 97
    val sdp =
      s"""v=0
         |o=- 0 0 IN IP4 $serverIp
         |s=Play
         |c=IN IP4 $serverIp
         |t=0 0
         |m=video $serverMediaPort TCP/RTP/AVP 96 98 97
         |a=sendrecv
         |a=rtpmap:96 PS/90000
         |a=rtpmap:98 H264/90000
         |a=rtpmap:97 MPEG4/90000
         |a=setup:passive
         |a=connection:new
         |y=$tcpSsrc
         |f=""".stripMargin

    val transportProtocol = if (sipIsUdp) "SIP/2.0/UDP" else "SIP/2.0/TCP"

    val bytes = sdp.getBytes(CharsetUtil.UTF_8)
    val buf = Unpooled.wrappedBuffer(bytes)
//    val buf = Unpooled.copiedBuffer(sdp, CharsetUtil.UTF_8)

//    val body = Unpooled.copiedBuffer(sdp, CharsetUtil.UTF_8)
    val request = new DefaultFullSipRequest(SipVersion.SIP_2_0, SipMethod.INVITE, s"sip:$id@$ip:$port", buf)

//    val request = new DefaultFullSipRequest(SipVersion.SIP_2_0, SipMethod.INVITE, s"sip:$id@$ip:$port")
//    request.content().writeBytes(buf)
//    buf.release()
    val headers = request.headers()

    headers.set(SipHeaderNames.CALL_ID, s"$id@$serverIp")
    headers.set(SipHeaderNames.CSEQ, s"1 ${SipMethod.INVITE.name()}")
    headers.set(SipHeaderNames.FROM, s"<sip:$serverId@${serverId.take(10)}>;tag=$id")
    headers.set(SipHeaderNames.TO, s"<sip:$id@${id.take(10)}>")
    headers.set(SipHeaderNames.VIA, s"$transportProtocol $ip:$port;rport")
    headers.set(SipHeaderNames.CONTACT, s"<sip:$id@$serverIp:$serverPort>")
//    headers.set(SipHeaderNames.CONTACT, s"<sip:$id@$ip:$port>")
    headers.set(SipHeaderNames.MAX_FORWARDS, 70)
    headers.set(SipHeaderNames.CONTENT_TYPE, s"Application/SDP")
    headers.set(SipHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
    request.setRecipient(recipientAddress)
    request
  }
}

object GBDevice {
  def getPlayStream(sourceId: GBSourceId): GB28181PlayStream = {
    import scala.concurrent.ExecutionContext.Implicits.global
    GB28181PlayStream.getOrElseSubmit(sourceId, id => new GB28181PlayStream(id, new GB28181RealtimeTCPSource(), new GB28181ConsumptionPipeline))
  }

  def getGBPublisher(ps: GB28181PlayStream): GBStreamPublisher = ps.getOrElseAddConsumer(classOf[GBStreamPublisher], {
    val publisher = new GBStreamPublisher()
    new Thread(publisher).start()
    publisher
  })
}
