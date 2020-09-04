package com.getouo.gb.scl.sip

import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

import io.netty.channel.{Channel, ChannelId}
import io.netty.channel.group.{ChannelGroup, DefaultChannelGroup}
import io.netty.util.concurrent.GlobalEventExecutor

import scala.jdk.CollectionConverters._

object ChannelGroups {

  private def createGroup(): ChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

  private val DEFAULT_GROUP_NAME = "DEFAULT_CHANNEL_GROUP"
  private val defaultGroup: ChannelGroup = createGroup()

  private val channelGroups: ConcurrentHashMap[String, ChannelGroup] = new ConcurrentHashMap[String, ChannelGroup]()
  channelGroups.put(DEFAULT_GROUP_NAME, defaultGroup)

  def addChannel(channel: Channel): Unit = defaultGroup.add(channel)

  def addChannel(channel: Channel, group: String): Unit = {
    addChannel(channel)
    if (group != DEFAULT_GROUP_NAME)
      channelGroups.computeIfAbsent(group, _ => createGroup()).add(channel)
  }

  def find(channelId: ChannelId): Option[Channel] = Option.apply(defaultGroup.find(channelId))

  def filter(p: Channel => Boolean = _ => true): Set[Channel] = {
    val channels: util.Set[Channel] = defaultGroup.stream().filter(channel => p.apply(channel)).collect(Collectors.toSet[Channel])
    channels.asScala.toSet
  }

  def filterFirst(p: Channel => Boolean): Option[Channel] = {
    //    import scala.util.control.Breaks._
    val it = filter().iterator
    var c: Channel = null
    while (it.hasNext && c == null) {
      val channel = it.next()
      if (p.apply(channel)) c = channel
    }
    Option.apply(c)
  }

//  def findBySubjectId(subjectId: String): Option[Channel] = filterFirst(c => Option.apply(c.attr(Attributes.PRINCIPAL).get()).exists(_.subjectId == subjectId))

}
