package com.getouo.gb.scl.service

import java.util.concurrent.TimeUnit

import com.getouo.gb.configuration.PlatConfiguration
import com.getouo.gb.scl.model.GBDevice
import com.getouo.gb.scl.server.SipUdpServer
import com.getouo.gb.scl.sip.SipMessageTemplate
import io.pkts.packet.sip.SipMessage
import org.springframework.stereotype.Service

import scala.util.{Failure, Success, Try}

@Service
class DeviceService(redis: RedisService, udpServer: SipUdpServer, platCfg: PlatConfiguration) {

  def findDevice(id: String): Option[GBDevice] = Try(redis.get[GBDevice](id)) match {
    case Failure(_) => None
    case Success(value) => Option.apply(value)
  }

  // 设备信息刷新
  def keepalive(id: String, put: Option[GBDevice] => GBDevice): Unit =
    redis.setKVTimeLimit(id, put.apply(findDevice(id)), 60, TimeUnit.SECONDS)

  implicit def defaultTrue(u: Unit): Boolean = true

  def offline(id: String): Option[GBDevice] = findDevice(id).filter(d => redis.removeValue(d.id))

  def play(id: String): Unit = {
    findDevice(id) match {
      case None => throw new IllegalArgumentException(s"device [$id] is not found")
      case Some(device) =>
        device.sipConnection() match {
          case None =>
          case Some(conn) =>
            val strInvite = SipMessageTemplate.inviteRequest(id, conn.getRemoteIpAddress, conn.getRemotePort, callId = id,
              platCfg.getId, conn.getLocalIpAddress, conn.getLocalPort, id.substring(id.length()-4), 0)
            val playMessage = SipMessage.frame(strInvite)
            System.err.println(
              s"""
                 |-------playMessage
                 |$playMessage
                 |""".stripMargin)
            conn.send(playMessage)
        }
    }
  }
}
