package com.getouo.gb.scl.util

import java.util.concurrent.atomic.AtomicInteger

import com.getouo.gb.scl.server.UdpPusher

object ConstVal {

  sealed trait RtpTransport {
    protected def requestValue: String
    def transportValue(): String

    private val transportLowerCase: String = requestValue.toLowerCase
    val transportParams: Array[String] = transportLowerCase.split(";").map(_.trim)
    val transTypeOpt: Option[String] = transportParams.find(_.contains("rtp/avp"))
    val isUdp: Boolean = transportLowerCase.contains("rtp/avp") && transportParams.exists(_.startsWith("client_port"))
    val isTcp: Boolean = transportLowerCase.contains("tcp") && transportLowerCase.contains("interleaved=")
  }

  object RtpTransport {
    def valueOf(transport: String): RtpTransport = {
      val ts = CustomRtpTransport(transport)
      if (ts.isUdp) {
        val udpPusher = SpringContextUtil.getBean(clazz = classOf[UdpPusher]).getOrElse(throw new Exception(s"获取UdpPusher失败"))
        ConstVal.RtpTransportOverUDP(transport).updateServerPort(udpPusher.port)
      } else if (ts.isTcp) {
        RtpTransportOverTCP(transport)
      } else ts
    }
  }

  case class CustomRtpTransport(requestValue: String, v2transport: String => String = v => v) extends RtpTransport {
    override def transportValue(): String = v2transport(requestValue)
  }

  /**
   *
   * @param requestValue header 值
   */
  case class RtpTransportOverUDP(requestValue: String) extends RtpTransport {

    private val serverPort: AtomicInteger = new AtomicInteger(0)

    def updateServerPort(port: Int): RtpTransportOverUDP = {
      serverPort.set(port)
      this
    }

    // unicast：表示单播，如果是multicast则表示多播
    val castType: String = transportParams.find(_.contains("unicast")).getOrElse("multicast")
    val clientPortL: Int = transportParams.find(_.startsWith("client_port=")).get.split("=")(1).split("-")(0).toInt

    private def spValue(): String = {
      val portBuf = serverPort.get()
      if (portBuf > 0 && portBuf < 65535) s";server_port=${portBuf}-${portBuf + 1}" else ""
    }

    override def transportValue(): String = {
      if (requestValue.contains("server_port=")) requestValue
      else s"$requestValue${spValue()}"
    }
  }

  case class RtpTransportOverTCP(requestValue: String) extends RtpTransport {
    //    override def transportValue(): String = s"$value;unicast;interleaved=0-1"

    def channelNo: Array[Int] = transportParams.find(_.startsWith("interleaved=")).get.split("=")(1).split("-").map(_.toInt)
    def channel1: Int = channelNo(0)
    def channel2: Int = channelNo(1)

    override def transportValue(): String = requestValue
  }

  case object UnknownTransport extends RtpTransport {
    override def requestValue: String = ""
    override def transportValue(): String = "unknown"

  }

}
