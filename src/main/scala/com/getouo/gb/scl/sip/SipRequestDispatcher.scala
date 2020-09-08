package com.getouo.gb.scl.sip

import java.nio.charset.Charset

import com.getouo.gb.scl.model.GBDevice
import com.getouo.gb.scl.service.DeviceService
import com.getouo.gb.scl.util.{LogSupport, SpringContextUtil}
import com.getouo.sip.{FullSipRequest, SipHeaderNames, SipMethod, SipResponseStatus}
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import org.apache.commons.codec.digest.DigestUtils

import scala.util.{Failure, Success, Try}
import scala.xml.{Node, XML}

class SipRequestDispatcher extends SimpleChannelInboundHandler[FullSipRequest] with LogSupport {
//class SipRequestDispatcher extends SimpleChannelInboundHandler[DefaultSipRequest] with LogSupport {
  override def channelRead0(ctx: ChannelHandlerContext, msg: FullSipRequest): Unit = {
    val channel = ctx.channel()

    logger.info("[{}{}] rec request msg", channel.id().asShortText(), msg)

    val method: SipMethod = msg.method()
    method match {
      case SipMethod.REGISTER => handlerRegister(channel, msg)
      case SipMethod.MESSAGE => handlerMessage(channel, msg)
      case SipMethod.BYE => println(s"bye: $msg")
    }
  }

  private def handlerMessage(channel: Channel, req: FullSipRequest): Unit = {
    val reqBodyStr1 = req.content().toString()
    val reqBodyStr = req.content().toString(Charset.forName("gbk"))
    logger.info(
      s"""
         |handlerMessage:req.content().toString=
         |${reqBodyStr}
         |----------------------
         |${reqBodyStr1}
         |""".stripMargin)
    Try(XML.loadString(reqBodyStr)) match {
      case Failure(exception) =>
        logger.error(s"解析请求消息失败: ${exception.getMessage}")
        val sipResponse = req.createResponse(SipResponseStatus.BAD_REQUEST)
        sipResponse.headers().set(SipHeaderNames.REASON, s"xml parse error: ${exception.getMessage}")
      case Success(xml) =>
        val cmdOpt: Option[Node] = (xml \ "CmdType").headOption
        val deviceIdOpt: Option[Node] = (xml \ "DeviceID").headOption
        val service = SpringContextUtil.getBean(clazz = classOf[DeviceService]).getOrElse(throw new RuntimeException("获取不到redis服务"))

//        val connection = event.getConnection
//        val udpOpt = if (connection.isUDP) {
//          val udpConn = connection.asInstanceOf[UdpConnection]
//          Some((udpConn.getRemoteIpAddress, udpConn.getRemotePort))
//        } else None

        if (cmdOpt.exists(_.text == "Keepalive") && deviceIdOpt.isDefined) {
          val deviceId = deviceIdOpt.map(_.text).get
          service.keepalive(deviceId, oldOpt => {

            val isUdp = channel.isInstanceOf[NioDatagramChannel]
            val udpOpt = if(isUdp) Some(req.recipient().getAddress.getHostAddress, req.recipient().getPort) else None
            if (!isUdp) ChannelGroups.addChannel(channel)

            if (!isUdp) {
              ChannelGroups.addChannel(channel)
            }

            oldOpt.map(_.copy(tcpOpt = if (!isUdp) Some(channel.id()) else None, udpAddrOpt = udpOpt))
              .getOrElse(GBDevice(deviceId, if (!isUdp) Some(channel.id()) else None, udpOpt))
          })
          val response = req.createResponse(SipResponseStatus.OK)
          channel.writeAndFlush(response)
        } else {
          logger.info(
            s"""
               |【handlerRequest: message】+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
               |$req
               |+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
               |""".stripMargin)
        }
    }
  }

  private def handlerRegister(channel: Channel, req: FullSipRequest): Unit = {
    val headers = req.headers()
    val callId = headers.get(SipHeaderNames.CALL_ID)
    val authorizationStrings = headers.getAll(SipHeaderNames.AUTHORIZATION)
    val authorizationStr = headers.get(SipHeaderNames.AUTHORIZATION)

    val expires = headers.get(SipHeaderNames.EXPIRES)
    val from = headers.get(SipHeaderNames.FROM)

    val p = s".*sip:([0-9]{20})@([0-9]{10}).*".r
    val p(deviceId, domain) = from
    val hasAuthorizationHeader = authorizationStr == null || authorizationStr.isEmpty
    System.err.println(
      s"""authorizationStrings ========== ${authorizationStrings}
         |authorizationStr ========== ${authorizationStr}
         |hasAuthorizationHeader ========== ${hasAuthorizationHeader}
         |""".stripMargin)

    if (hasAuthorizationHeader) {

      val response = req.createResponse(SipResponseStatus.UNAUTHORIZED)
      val nonce = DigestUtils.md5Hex(callId + deviceId)
      val respHeaders = response.headers()
      respHeaders.set(SipHeaderNames.WWW_AUTHENTICATE, s"""Digest realm="4305000098", nonce="$nonce"""")
      logger.info(
        s"""
           |register response ========== ${hasAuthorizationHeader}
           |${response}
           |""".stripMargin)
      channel.writeAndFlush(response)
    } else {
      val response = req.createResponse(SipResponseStatus.OK)
      response.headers().set(SipHeaderNames.EXPECT, 3600)
      ChannelGroups.addChannel(channel)
      channel.writeAndFlush(response)
    }

  }
}
