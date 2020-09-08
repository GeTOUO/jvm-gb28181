package com.getouo.gb.scl.service

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import com.getouo.gb.configuration.PlatConfiguration
import com.getouo.gb.scl.io.GB28181RealtimeTCPSource
import com.getouo.gb.scl.model.{GBDevice, SipConnection}
import com.getouo.gb.scl.server.GBStreamPublisher
import com.getouo.gb.scl.sip.ChannelGroups
import com.getouo.gb.scl.stream.{GB28181ConsumptionPipeline, GB28181PlayStream, GBSourceId}
import com.getouo.gb.scl.util.{ChannelUtil, NetAddressUtil}
import com.getouo.sip.FullSipRequest
import io.netty.channel.Channel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.util.concurrent.Future
import org.springframework.stereotype.Service

import scala.util.{Failure, Success, Try}

@Service
class DeviceService(redis: RedisService, platCfg: PlatConfiguration) {

  def findDevice(id: String): Option[GBDevice] = Try(redis.get[GBDevice](id)) match {
    case Failure(_) => None
    case Success(value) => Option.apply(value)
  }

  // 设备信息刷新
  def keepalive(id: String, channel: Channel, req: FullSipRequest): Unit = {
    val recipient: InetSocketAddress = req.recipient()
    val isUdp = channel.isInstanceOf[NioDatagramChannel]
    if (isUdp) ChannelGroups.addChannel(channel, ChannelGroups.SIP_UDP_POINT)
    else ChannelGroups.addChannel(channel)

    val device = findDevice(id) match {
      case Some(device) => device.copy(recipientAddress = recipient, connection = SipConnection(isUdp, channel.id()))
      case None => GBDevice(id, recipient, SipConnection(isUdp, channel.id()))
    }
    device.keepalive(req)
    redis.setKVTimeLimit(id, device, 60, TimeUnit.SECONDS)
  }

  private implicit def defaultTrue(u: Unit): Boolean = true

  def offline(id: String): Option[GBDevice] = findDevice(id).filter(d => redis.removeValue(d.id))

  def play(id: String): String = {
    findDevice(id) match {
      case Some(device) => device.play(platCfg.getId)
      case None => s"device [$id] is not found"
    }
  }
}
