package com.getouo.gb.scl.model

import java.net.InetSocketAddress

import com.getouo.gb.scl.server.SipUdpServer
import com.getouo.gb.scl.sip.ChannelGroups
import com.getouo.gb.scl.util.SpringContextUtil
import io.netty.channel.ChannelId
import io.sipstack.netty.codec.sip.{Connection, TcpConnection, UdpConnection}

case class GBDevice(id: String, tcpOpt: Option[ChannelId], udpAddrOpt: Option[(String, Int)]) {

  def sipConnection(): Option[Connection] =
    udpAddrOpt match {
      case Some((str, i)) => Some(new UdpConnection(GBDevice.udpServer.sender, new InetSocketAddress(str, i)))
      case None => tcpOpt.flatMap(ChannelGroups.find).map(channel =>
        new TcpConnection(channel, channel.remoteAddress.asInstanceOf[InetSocketAddress]))
    }
}

object GBDevice {
  private val udpServer: SipUdpServer = SpringContextUtil.getBean(clazz = classOf[SipUdpServer]).getOrElse(throw new Exception("无法加载 SipUdpServer"))
}