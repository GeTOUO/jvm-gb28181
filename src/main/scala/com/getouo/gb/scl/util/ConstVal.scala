package com.getouo.gb.scl.util

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
   * @param sPort
   * @param targetIp
   * @param targetPort
   * @param castType unicast：表示单播，如果是multicast则表示多播
   */
  case class RtpOverUDP(sIp: String, sPort: Int, targetIp: String, targetPort: Int, castType: String = "unicast") extends RtpTransType {
    override val value = "RTP/AVP"
    private def spValue(): String = if (sPort > 0 && sPort < 65535) ";server_port=${sPort}-${sPort + 1}" else ""
    override def transportValue(): String = s"$value;$castType; client_port=$targetPort-${targetPort+1}${spValue()}"
  }

  case class RtpOverTCP() extends RtpTransType {
    override val value: String = "RTP/AVP/TCP"
    override def transportValue(): String = s"$value;unicast;interleaved=0-1"
  }

  case object UnknownTransType extends RtpTransType {
    override val value: String = "unknown"
    override def transportValue(): String = "unknown"
  }

}
