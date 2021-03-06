package com.getouo.gb.scl.util

import com.getouo.gb.scl.stream.{PlayStream, SourceConsumer, SourceId}
import com.getouo.gb.scl.util.ConstVal.{RtpTransport, UnknownTransport}
import io.netty.channel.Channel
import io.netty.util.AttributeKey

sealed trait Session {
  val id: SourceId // fileName or ChannelId
  val playStreamOpt: Option[PlayStream[_, _, _, _]]
  val consumerOpt: Option[SourceConsumer[_]]

  def idHash(): Long = id.idHash()
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

case class RtpSession(id: SourceId, pt: RtpTransport = UnknownTransport,
                      playStreamOpt: Option[PlayStream[_, _, _, _]], consumerOpt: Option[SourceConsumer[_]]) extends Session {
}
