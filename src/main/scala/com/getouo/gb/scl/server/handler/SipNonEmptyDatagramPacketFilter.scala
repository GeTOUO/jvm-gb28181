package com.getouo.gb.scl.server.handler

import java.nio.charset.Charset

import com.getouo.gb.configuration.PlatConfiguration
import com.getouo.gb.scl.util.LogSupport
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.{Channel, ChannelFuture, ChannelHandlerContext, SimpleChannelInboundHandler}
import org.springframework.stereotype.Component

//@Component
class SipNonEmptyDatagramPacketFilter extends SimpleChannelInboundHandler[DatagramPacket] with LogSupport {

  private var channel: Channel = null

  override def channelRead0(ctx: ChannelHandlerContext, packet: DatagramPacket): Unit = {
    val content = packet.content
    val gbkContent = content.toString(Charset.forName("gbk"))

    logger.info(s"->>>>>>>>>>>>>>> ${gbkContent.length}")

//    if (gbkContent.trim.isEmpty) {
//      println("空包")
//      return
//    }
//
//    val toP =
//      s"""
//         |收到:
//         |$gbkContent
//         |""".stripMargin
//
//    println(toP)

    ctx.fireChannelRead(packet.copy())
  }

  def setChannelFuture(c: ChannelFuture): Unit = {
    this.channel = c.channel()
  }
}
