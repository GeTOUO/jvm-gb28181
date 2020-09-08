package com.getouo.gb.scl.model

import java.net.InetSocketAddress

import com.getouo.gb.scl.server.HybridProtocolInstructionServer
import com.getouo.gb.scl.sip.ChannelGroups
import com.getouo.sip.{DefaultFullSipRequest, FullSipRequest, SipMethod, SipRequest, SipVersion}
import io.netty.channel.ChannelId
import io.sipstack.netty.codec.sip.{Connection, TcpConnection, UdpConnection}

case class GBDevice(id: String, ip: String, port: Int, tcpOpt: Option[ChannelId], udpAddrOpt: Option[(String, Int)]) {

  def sipConnection(): Option[Connection] =
    udpAddrOpt match {
      case Some((str, i)) => Some(new UdpConnection(HybridProtocolInstructionServer.getSipUdpSender(5060), new InetSocketAddress(str, i)))
      case None => tcpOpt.flatMap(ChannelGroups.find).map(channel =>
        new TcpConnection(channel, channel.remoteAddress.asInstanceOf[InetSocketAddress]))
    }

  def inviteMessage(serverIp: String, serverPort: Int, serverId: String): FullSipRequest = {

    val s = ""

    new DefaultFullSipRequest(SipVersion.SIP_2_0, SipMethod.INVITE, s"sip:$id@$ip:$port")
  }
}

//object GBDevice {
//  private val udpServer: SipUdpServer = SpringContextUtil.getBean(clazz = classOf[SipUdpServer]).getOrElse(throw new Exception("无法加载 SipUdpServer"))
//}
