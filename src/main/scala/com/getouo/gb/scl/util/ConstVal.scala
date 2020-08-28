package com.getouo.gb.scl.util

import java.util.concurrent.atomic.AtomicInteger

object ConstVal {

  sealed trait RtpTransType {
    val value: String
    def transportValue(): String
  }

  case class CustomRtpTransType(value: String, v2transport: String => String = v => v) extends RtpTransType {
    override def transportValue(): String = v2transport(value)
  }

  object RtpTransType {
    def valueOf(ts: String): CustomRtpTransType = CustomRtpTransType(ts)
  }

  /**
   *
   * @param sIp
   * @param targetIp
   * @param targetPort
   * @param castType unicast：表示单播，如果是multicast则表示多播
   */
  case class RtpOverUDP(sIp: String, targetIp: String, targetPort: Int, castType: String = "unicast") extends RtpTransType {
    override val value = "RTP/AVP"
    private val serverPort: AtomicInteger = new AtomicInteger(0)
    def updateServerPort(port: Int): RtpOverUDP = {
      serverPort.set(port)
      this
    }
    private def spValue(): String = {
      val portBuf = serverPort.get()
      if (portBuf > 0 && portBuf < 65535) s";server_port=${portBuf}-${portBuf + 1}" else ""
    }
    override def transportValue(): String = s"$value;$castType;client_port=$targetPort-${targetPort+1}${spValue()}"
  }

  case class RtpOverTCP(tv: String) extends RtpTransType {
    override val value: String = "RTP/AVP/TCP"
//    override def transportValue(): String = s"$value;unicast;interleaved=0-1"
    override def transportValue(): String = tv
  }

  case object UnknownTransType extends RtpTransType {
    override val value: String = "unknown"
    override def transportValue(): String = "unknown"
  }

}
