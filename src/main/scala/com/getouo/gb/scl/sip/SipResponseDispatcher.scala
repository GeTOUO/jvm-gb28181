package com.getouo.gb.scl.sip

import com.getouo.gb.scl.stream.{GB28181PlayStream, GBSourceId}
import com.getouo.gb.scl.util.LogSupport
import com.getouo.sip._
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

class SipResponseDispatcher extends SimpleChannelInboundHandler[FullSipResponse] with LogSupport {
  override def channelRead0(ctx: ChannelHandlerContext, resp: FullSipResponse): Unit = {
    logger.info(
      s"""
         |rec response message:
         |${resp}
         |""".stripMargin)
    val isFinal = resp.status().code() >= 200


    val headers = resp.headers()
    val inviteCSEQUpperCase = headers.get(SipHeaderNames.CSEQ).toUpperCase
    val isInvite = inviteCSEQUpperCase.endsWith(SipMethod.INVITE.name())
    if (isFinal && isInvite) {

      val tagR = ".*tag=(.*)".r
      val tagR(tag) = headers.get(SipHeaderNames.FROM)

//      val ack = SipMessageTemplate.generateAck(resp)

      val ack = new DefaultSipRequest(SipVersion.SIP_2_0, SipMethod.ACK, headers.get(SipHeaderNames.CONTACT), resp.recipient())
      val ackHeaders = ack.headers()
      ackHeaders.set(SipHeaderNames.CSEQ, inviteCSEQUpperCase.split(" ")(0) + " " + SipMethod.ACK.name())
      ackHeaders.set(SipHeaderNames.VIA, headers.get(SipHeaderNames.VIA))
      ackHeaders.set(SipHeaderNames.FROM, headers.get(SipHeaderNames.FROM))
      ackHeaders.set(SipHeaderNames.CALL_ID, headers.get(SipHeaderNames.CALL_ID))
      ackHeaders.set(SipHeaderNames.TO, headers.get(SipHeaderNames.TO))

      GB28181PlayStream.byIdOpt(GBSourceId(tag.toString, tag.toString)).foreach(f => {
        f.source.sdpInfo.set(resp.content().toString)
      })

      ctx.channel().writeAndFlush(ack)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    logger.error(s"err: ${cause.getMessage}")
  }
}
