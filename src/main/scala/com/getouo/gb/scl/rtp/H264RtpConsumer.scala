package com.getouo.gb.scl.rtp

import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.getouo.gb.scl.io.{EndSymbol, H264NaluData, H264SourceData}
import com.getouo.gb.scl.stream.{ConsumptionPipeline, SourceConsumer}
import com.getouo.gb.scl.util.LogSupport
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.util.concurrent.GlobalEventExecutor

import scala.jdk.CollectionConverters._
import scala.collection.{concurrent, mutable}

class H264RtpConsumer extends SourceConsumer[H264NaluData] with LogSupport {

  private val sendSeq: AtomicInteger = new AtomicInteger(0)
  private var timestamp: Int = 0
  private val framerate: Float = 25
  private val timestampIncrement: Int = (90000 / framerate).intValue() //+0.5
  // udp 订阅, udp-channel -> (ip, port)
  private val udpSubscriber: concurrent.Map[Channel, mutable.HashSet[(String, Int)]] =
    new ConcurrentHashMap[Channel, mutable.HashSet[(String, Int)]]().asScala
  // tcp 订阅
  private val tcpSubscriber: ChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

  override def onNext(pipeline: ConsumptionPipeline[_, H264NaluData], data: H264NaluData): Unit = {
    timestamp += timestampIncrement
    data.rtpPacket(sendSeq, timestamp).map(Unpooled.copiedBuffer).foreach(bf => {
      tcpSubscriber.writeAndFlush(bf)
      udpSubscriber.foreach { case (channel, subscribers) =>
        subscribers.map(info => {
          bf.retain(); new DatagramPacket(bf, new InetSocketAddress(info._1, info._2))
        }).foreach(addr => channel.writeAndFlush(addr))
      }
    })
  }

  override def onError(pipeline: ConsumptionPipeline[_, H264NaluData], thr: Throwable): Unit =
    logger.error(s"H264RtpConsumer err message: ${thr.getMessage}")

  override def onComplete(pipeline: ConsumptionPipeline[_, H264NaluData]): Unit = pipeline.unsubscribe(this)
}
