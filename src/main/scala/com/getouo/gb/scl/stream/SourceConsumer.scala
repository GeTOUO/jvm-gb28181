package com.getouo.gb.scl.stream

import java.util.concurrent.ConcurrentHashMap

import com.getouo.gb.scl.io.ISourceData
import com.getouo.gb.scl.util.LogSupport
import io.netty.channel.Channel
import io.netty.channel.group.{ChannelGroup, DefaultChannelGroup}
import io.netty.util.concurrent.GlobalEventExecutor

import scala.collection.{concurrent, mutable}
import scala.jdk.CollectionConverters._

trait SourceConsumer[S <: ISourceData] extends LogSupport {

  // udp 订阅, udp-channel -> (ip, port)
  protected val udpSubscriber: concurrent.Map[Channel, mutable.HashSet[(String, Int)]] =
    new ConcurrentHashMap[Channel, mutable.HashSet[(String, Int)]]().asScala
  // tcp 订阅
//  protected val tcpSubscriber: ChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
  protected val tcpSubscriber: DefaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

  def onNext(pipeline: ConsumptionPipeline[_, S], data: S): Unit

  def onError(pipeline: ConsumptionPipeline[_, S], thr: Throwable): Unit

  def onComplete(pipeline: ConsumptionPipeline[_, S]): Unit

  def tcpJoin(channel: Channel): Unit = {
    tcpSubscriber.add(channel)
    logger.info(s"tcp channel 已加入 ${getClass.getSimpleName}")
  }

  def udpJoin(sender: Channel, targetIpAndPort: (String, Int)): Unit = {
    udpSubscriber.getOrElseUpdate(sender, new mutable.HashSet[(String, Int)]).add(targetIpAndPort)
    logger.info(s"udp target-[ip:${targetIpAndPort._1},port:${targetIpAndPort._2}] 已加入 ${getClass.getSimpleName}")
  }
}
