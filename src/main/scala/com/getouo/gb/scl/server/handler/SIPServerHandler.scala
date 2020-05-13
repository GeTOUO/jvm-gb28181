package com.getouo.gb.scl.server.handler

import java.nio.charset.Charset

import com.getouo.gb.configuration.PlatConfiguration
import io.netty.channel.{Channel, ChannelFuture, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.channel.socket.DatagramPacket
import org.springframework.stereotype.Component

@Component
class SIPServerHandler(configuration: PlatConfiguration) extends SimpleChannelInboundHandler[DatagramPacket]{

  private var channel: Channel = null

  override def channelRead0(ctx: ChannelHandlerContext, packet: DatagramPacket): Unit = {
    val gbkContent = packet.content.toString(Charset.forName("gbk"))
    if (gbkContent.trim.isEmpty) {
      println("空包")
      return
    }

    val senderHostAddress = packet.sender().getAddress.getHostAddress
    val senderPort = packet.sender().getPort
    val toP =
      s"""
        |收到: ${senderHostAddress}:${senderPort}
        |$gbkContent
        |""".stripMargin

    println(toP)
  }

  def setChannelFuture(c: ChannelFuture): Unit = {
    this.channel = c.channel()
  }
}
