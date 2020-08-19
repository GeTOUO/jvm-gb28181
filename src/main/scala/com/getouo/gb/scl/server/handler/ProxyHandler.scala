package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.util.LogSupport
import gov.nist.javax.sip.header.SIPHeaderNames
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.pkts.buffer.Buffers
import io.pkts.packet.sip.header.SipHeader
import io.pkts.packet.sip.header.impl.SipHeaderImpl
import io.pkts.packet.sip.{SipRequest, SipResponse}
import io.sipstack.netty.codec.sip.SipMessageEvent
import org.apache.commons.codec.digest.DigestUtils

class ProxyHandler extends SimpleChannelInboundHandler[SipMessageEvent] with LogSupport {
  override def channelRead0(ctx: ChannelHandlerContext, event: SipMessageEvent): Unit = {
    val msg = event.getMessage
    msg match {
      //      case request: SipRequest => ctx.writeAndFlush(handlerRequest(request))
      case request: SipRequest => handlerRequest(ctx.channel(), request, event)
      case response: SipResponse => logger.error(s"不接受响应")
      case _ => logger.error(s"不接受未知消息")
    }
  }

  private def handlerRequest(channel: Channel, req: SipRequest, event: SipMessageEvent): SipResponse = {

    logger.info(
      s"""
         |【handlerRequest】+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
         |$req
         |+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
         |""".stripMargin)

    if (req.isRegister) {
      val response = handleRegister(req)
      if (response != null) {
        logger.info(
          s"""
             |///////////////////////////
             |$response
             |///////////////////////////
             |""".stripMargin)
        event.getConnection.send(response)
      }

      //      sipResponse.getReasonPhrase
    } else if (req.isMessage) {
      println(s"req.getContent =${req.getContent}")
      println(s"req.getContent =${req.getContent.getClass}")

    }
    null
  }

  private def handleRegister(req: SipRequest): SipResponse = {
    val callId = req.getCallIDHeader.getCallId.toString

    @scala.annotation.tailrec
    def authorizationValues(container: Seq[String] = Nil): Seq[String] = {
      val header = req.popHeader(Buffers.wrap(SIPHeaderNames.AUTHORIZATION))
      if (header != null) {
        authorizationValues(container :+ header.getValue.toString)
      } else container
    }

    val av = authorizationValues()
    val authorizations: Option[SipHeader] = if (av.isEmpty) None else
      Some(new SipHeaderImpl(Buffers.wrap(SIPHeaderNames.AUTHORIZATION), Buffers.wrap(av.reduce((a, b) => a + ", " + b))))

    val expires = req.getExpiresHeader.getExpires

    val p = s".*sip:([0-9]{20})@([0-9]{10}).*".r
    val p(deviceId, domain) = req.getFromHeader.getAddress.getURI.toString

    System.err.println(s"authorization ========== ${authorizations}")

    (req.getCSeqHeader.getSeqNumber, req.getCSeqHeader.getMethod.toString) match {
      case (1, "REGISTER") =>
        val sipResponse = req.createResponse(401)
        val nonce = DigestUtils.md5Hex(callId + deviceId)
        val wwwBuffer = Buffers.wrap(s"""Digest realm="4305000098", nonce="$nonce"""")
        sipResponse.setHeader(new SipHeaderImpl(Buffers.wrap(SIPHeaderNames.WWW_AUTHENTICATE), wwwBuffer))
        sipResponse
      case (2, "REGISTER") =>
//        authorization

        val sipResponse = req.createResponse(200)
        sipResponse.setHeader(new SipHeaderImpl(Buffers.wrap(SIPHeaderNames.EXPIRES), Buffers.wrap(3600)))
        sipResponse
      case (3, "REGISTER") =>

        null
    }
  }
}
