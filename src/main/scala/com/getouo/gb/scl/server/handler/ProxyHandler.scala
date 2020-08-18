package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.util.LogSupport
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.pkts.packet.sip.{SipRequest, SipResponse}
import io.pkts.packet.sip.impl.{SipMessageImpl, SipRequestImpl, SipResponseImpl}
import io.sipstack.netty.codec.sip.SipMessageEvent

class ProxyHandler extends SimpleChannelInboundHandler[SipMessageEvent] with LogSupport {
  override def channelRead0(channelHandlerContext: ChannelHandlerContext, event: SipMessageEvent): Unit = {
    val msg = event.getMessage

    msg match {
      case impl: SipMessageImpl =>
        impl match {
          case impl: SipRequestImpl =>
          case impl: SipResponseImpl =>
          case _ =>
        }
      case request: SipRequest =>
        request match {
          case impl: SipRequestImpl =>
          case _ =>
        }
      case response: SipResponse =>
      case _ =>
    }
    if (msg.isRequest) {
      logger.info(
        s"""
           | sip rerquest :
           | ${msg.getMethod}
           | ${msg.getMethod.getClass}
           | $msg
           |""".stripMargin)
    } else {
      logger.info(
        s"""
           | sip response :
           | $msg
           |""".stripMargin)
    }
  }
}
