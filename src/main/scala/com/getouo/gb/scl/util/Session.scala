package com.getouo.gb.scl.util

import com.getouo.gb.scl.stream.{PlayStream, SourceConsumer}
import com.getouo.gb.scl.util.ConstVal.{RtpTransType, UnknownTransType}
import io.netty.channel.Channel
import io.netty.util.AttributeKey

sealed trait Session {
  val id: String // fileName or ChannelId
  val playStreamOpt: Option[PlayStream[_, _, _, _]]
  val consumerOpt: Option[SourceConsumer[_]]

  def idHash(): Long = Integer.toUnsignedLong(id.hashCode)
}

object Session {
  val RTP_SESSION_ATTR_KEY: AttributeKey[Session] = AttributeKey.newInstance[Session]("rtp-session")

  def rtpSession(channel: Channel): Option[RtpSession] = {
    val session = channel.attr(RTP_SESSION_ATTR_KEY).get()
    session match {
      case rs@RtpSession(id, pt, playStreamOpt, consumerOpt) => Some(rs)
      case _ => None
    }
  }

  def updateChannelSession(channel: Channel, op: Option[Session] => Option[Session]): Unit = {
    val cak = channel.attr(RTP_SESSION_ATTR_KEY)
    val oldSessionOpt = cak.get()
    op.apply(Option.apply(oldSessionOpt)) match {
      case Some(newSession) => cak.set(newSession)
      case None => cak.set(null)
    }
  }
}

case class RtpSession(id: String, pt: RtpTransType = UnknownTransType,
                      playStreamOpt: Option[PlayStream[_, _, _, _]], consumerOpt: Option[SourceConsumer[_]]) extends Session {
}
