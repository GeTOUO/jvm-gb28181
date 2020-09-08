package com.getouo.gb.scl.server.handler

import java.net.InetSocketAddress

import com.getouo.gb.scl.model.GBDevice
import com.getouo.gb.scl.service.DeviceService
import com.getouo.gb.scl.sip.SipMessageTemplate
import com.getouo.gb.scl.stream.{GB28181PlayStream, GBSourceId}
import com.getouo.gb.scl.util.{LogSupport, SpringContextUtil}
import gov.nist.javax.sip.header.SIPHeaderNames
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.pkts.buffer.{Buffers, ByteBuffer}
import io.pkts.packet.sip.header.SipHeader
import io.pkts.packet.sip.header.impl.SipHeaderImpl
import io.pkts.packet.sip.{SipRequest, SipResponse}
import io.sipstack.netty.codec.sip.{SipMessageEvent, UdpConnection}
import org.apache.commons.codec.digest.DigestUtils

import scala.util.{Failure, Success, Try}
import scala.xml.{Node, XML}

class ProxyHandler extends SimpleChannelInboundHandler[SipMessageEvent] with LogSupport {
  override def channelRead0(ctx: ChannelHandlerContext, event: SipMessageEvent): Unit = {
    val msg = event.getMessage
    msg match {
      //      case request: SipRequest => ctx.writeAndFlush(handlerRequest(request))
      case request: SipRequest => handlerRequest(ctx.channel(), request, event)
      case response: SipResponse => handlerResponse(ctx.channel(), response, event)
      case _ => logger.error(s"不接受未知消息")
    }
  }

  private def handlerResponse(channel: Channel, resp: SipResponse, event: SipMessageEvent): Unit = {
    val tag = resp.getFromHeader.getTag
    logger.info(
      s"""
         |收到响应: tag=$tag
         |$resp
         |""".stripMargin)
    logger.info(
      s"""
         |收到响应: tag=$tag; content :
         |${resp.getRawContent}
         |""".stripMargin)

    if (resp.isFinal && resp.isInvite) {
      val ack = SipMessageTemplate.generateAck(resp)

      GB28181PlayStream.byIdOpt(GBSourceId(tag.toString, tag.toString)).foreach(f => {
        f.source.sdpInfo.set(resp.getRawContent.toString)
      })


      event.getConnection.send(ack)
    }


  }

  private def handlerRequest(channel: Channel, req: SipRequest, event: SipMessageEvent): SipResponse = {
    //


    if (req.isRegister) {
      val response = handleRegister(req)
      if (response != null) {
        event.getConnection.send(response)
      }

      //      sipResponse.getReasonPhrase
    } else if (req.isMessage) {

      var bodyStr = req.getContent.toString

      if (bodyStr.startsWith("\r\n")) {
        bodyStr = bodyStr.substring(2).appended('>')
      }

      Try(XML.loadString(bodyStr)) match {
        case Failure(exception) =>

          logger.error(
            s"""
               |解析请求消息失败: ${exception.getMessage}
               |${req}
               |${req.getContent.asInstanceOf[ByteBuffer].getClass}
               |${req.getContent}
               |${bodyStr}
               |""".stripMargin)
        case Success(xml) =>
          val xmlEmptyNode = <EmptyNode>EmptyNode</EmptyNode>
          val cmdOpt: Option[Node] = (xml \ "CmdType").headOption
          val deviceIdOpt: Option[Node] = (xml \ "DeviceID").headOption

          val service = SpringContextUtil.getBean(clazz = classOf[DeviceService]).getOrElse(throw new RuntimeException("获取不到redis服务"))
          val connection = event.getConnection
          val udpOpt = if (connection.isUDP) {
            val udpConn = connection.asInstanceOf[UdpConnection]
            Some((udpConn.getRemoteIpAddress, udpConn.getRemotePort))
          } else None

          if (cmdOpt.exists(_.text == "Keepalive") && deviceIdOpt.isDefined) {
            val deviceId = deviceIdOpt.map(_.text).get
//            service.keepalive(deviceId, channel, connection.getRemoteAddress)
//            service.keepalive(deviceId, channel, connection.getRemoteAddress)
//            val response = req.createResponse(200)
//            event.getConnection.send(response)
          } else {
            logger.info(
              s"""
                 |【handlerRequest】+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                 |$req
                 |+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                 |""".stripMargin)
          }
      }
    } else {
      logger.info(
        s"""
           |【handlerRequest】+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
           |$req
           |+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
           |""".stripMargin)
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
    val seqNumber = req.getCSeqHeader.getSeqNumber
    req.getCSeqHeader.getMethod.toString.trim.toUpperCase match {
      case "REGISTER" if authorizations.isEmpty =>
        val sipResponse = req.createResponse(401)
        val nonce = DigestUtils.md5Hex(callId + deviceId)
        val wwwBuffer = Buffers.wrap(s"""Digest realm="4305000098", nonce="$nonce"""")
        sipResponse.setHeader(new SipHeaderImpl(Buffers.wrap(SIPHeaderNames.WWW_AUTHENTICATE), wwwBuffer))
        sipResponse
      case "REGISTER" if authorizations.nonEmpty =>
        val sipResponse = req.createResponse(200)
        sipResponse.setHeader(new SipHeaderImpl(Buffers.wrap(SIPHeaderNames.EXPIRES), Buffers.wrap(3600)))
        sipResponse
      case _ => null
    }
  }
}


object XmlT {
  def main(args: Array[String]): Unit = {
    //    val list = <ul><li>Fred</li><li>Wilma</li></ul>


    val xmlStr =
      s"""<?xml version="1.0" encoding="GB2312"?>
         |<Notify>
         |<CmdType>Keepalive</CmdType>
         |<SN>825</SN>
         |<DeviceID>43050200981328000010</DeviceID>
         |<Status>OK</Status>
         |<Info>
         |</Info>
         |</Notify>
         |""".stripMargin
    val list = XML.loadString(xmlStr)


    val lists: scala.xml.Elem = <Notify>
      <CmdType>Keepalive</CmdType>
      <SN>431</SN>
      <DeviceID>43050200981328000010</DeviceID>
      <Status>OK</Status>
      <Info>
      </Info>
    </Notify>


    val titleNodes = list \ "DeviceID"

    titleNodes.foreach(n => println(s"${n.label} ; ${n.text};  ${n.child.getClass}"))

    val list2 = list.copy(label = "ol")
    println(list.find(_.label == "Notify"))
    println(list.getClass)
    println(list2)
  }
}
