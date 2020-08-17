package com.getouo.gb.scl.util

import java.net.{InetSocketAddress, SocketAddress}

import io.netty.channel.Channel

object ChannelUtil {

  private def getIp(sa: InetSocketAddress): String = sa.getAddress.getHostAddress

  def castSocketAddr(socketAddr: SocketAddress): InetSocketAddress = socketAddr.asInstanceOf[InetSocketAddress]

  def localIp(channel: Channel): String = getIp(channel.localAddress().asInstanceOf[InetSocketAddress])

  def remoteIp(channel: Channel): String = getIp(channel.remoteAddress().asInstanceOf[InetSocketAddress])
}
