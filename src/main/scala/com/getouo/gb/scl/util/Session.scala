package com.getouo.gb.scl.util

import com.getouo.gb.scl.util.ConstVal.{RtpTransType, UnknownTransType}
import io.netty.channel.Channel
import io.netty.util.AttributeKey

sealed trait Session {
  val id: String
}

object Session {
  val RTP_SESSION_ATTR_KEY: AttributeKey[Session] = AttributeKey.newInstance[Session]("rtp-session")

  def rtpSession(channel: Channel): Option[Session] = Option.apply(channel.attr(RTP_SESSION_ATTR_KEY).get())

  def updateChannelSession(channel: Channel, op: Option[Session] => Option[Session]): Unit = {
    val cak = channel.attr(RTP_SESSION_ATTR_KEY)
    val oldSessionOpt = cak.get()
    op.apply(Option.apply(oldSessionOpt)) match {
      case Some(newSession) => cak.set(newSession)
      case None => cak.set(null)
    }
  }
}

case class RtpSession(id: String, pt: RtpTransType = UnknownTransType) extends Session {
}
