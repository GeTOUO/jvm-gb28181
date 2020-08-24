package com.getouo.gb.scl.service

import java.util.concurrent.TimeUnit

import com.getouo.gb.configuration.PlatConfiguration
import com.getouo.gb.scl.io.GB28181RealtimeSource
import com.getouo.gb.scl.model.GBDevice
import com.getouo.gb.scl.server.{GBStreamPublisher, SipUdpServer}
import com.getouo.gb.scl.sip.SipMessageTemplate
import com.getouo.gb.scl.stream.{GB28181ConsumptionPipeline, GB28181PlayStream, GBSourceId}
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

  private implicit def defaultTrue(u: Unit): Boolean = true

  def offline(id: String): Option[GBDevice] = findDevice(id).filter(d => redis.removeValue(d.id))

  def play(id: String): String = {
    findDevice(id) match {
      case None => s"device [$id] is not found"
      case Some(device) =>
        device.sipConnection() match {
          case None => "设备无连接"
          case Some(conn) =>

            //            val localIp = conn.getLocalIpAddress
            val localIp = "192.168.2.19"
            val sourceId = GBSourceId(id, id)
            val ps: GB28181PlayStream = GB28181PlayStream.getOrElseUpdateLocalFileH264Stream(sourceId, id => {
              import scala.concurrent.ExecutionContext.Implicits.global
              val pipeline = new GB28181ConsumptionPipeline
              val stream = new GB28181PlayStream(id, new GB28181RealtimeSource(), pipeline)
              stream.submit()
              stream
            })

            val consumer: GBStreamPublisher = ps.getConsumerOrElseUpdate(classOf[GBStreamPublisher], {
              val publisher = new GBStreamPublisher()
              new Thread(publisher).start()
              publisher
            })

            val playMessage = SipMessageTemplate.inviteRequest(
              channel = id, deviceIp = conn.getRemoteIpAddress, devicePort = conn.getRemotePort, sipServerId = platCfg.getId,
              callId = id, sipIp = localIp, sPort = conn.getLocalPort, fromTag = id, mediaServerIp = localIp, mediaServerPort = ps.source.streamChannel.localPort)

            System.err.println(
              s"""
                 |-------playMessage
                 |$playMessage""".stripMargin)
            conn.send(playMessage)
            s"rtsp://192.168.2.19:${consumer.localPort}"
        }
    }
  }
}
