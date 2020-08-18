package com.getouo.gb.scl.rtp

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

import com.getouo.gb.scl.io.H264NaluData
import com.getouo.gb.scl.stream.{ConsumptionPipeline, SourceConsumer}
import com.getouo.gb.scl.util.LogSupport
import io.netty.buffer.Unpooled
import io.netty.channel.socket.DatagramPacket
import io.netty.util.ReferenceCountUtil
import io.netty.util.concurrent.Future

class H264RtpConsumer extends SourceConsumer[H264NaluData] with LogSupport {

  private val sendSeq: AtomicInteger = new AtomicInteger(0)
  private var timestamp: Int = 0
  private val framerate: Float = 25
  private val timestampIncrement: Int = (90000 / framerate).intValue() //+0.5


  var count = 0

  override def onNext(pipeline: ConsumptionPipeline[_, H264NaluData], data: H264NaluData): Unit = {
    timestamp += timestampIncrement
    count += 1
    val packets = data.rtpPacket(sendSeq, timestamp)
    packets.map(Unpooled.copiedBuffer).foreach(bf => {
      udpSubscriber.foreach { case (channel, subscribers) =>
        subscribers.map(info => {
          bf.retain()
          new DatagramPacket(bf, new InetSocketAddress(info._1, info._2))
        }).foreach(addr => channel.writeAndFlush(addr))
      }
      ReferenceCountUtil.release(bf)
    })
    packets.map(p => {
      val tcpHeader = new Array[Byte](4)
      tcpHeader(0) = '$'
      tcpHeader(1) = 0
      tcpHeader(2) = ((p.length & 0xFF00) >> 8).byteValue()
      tcpHeader(3) = (p.length & 0xFF).byteValue()
      tcpSubscriber.writeAndFlush(Unpooled.copiedBuffer(tcpHeader ++ p))
    })
  }

  override def onError(pipeline: ConsumptionPipeline[_, H264NaluData], thr: Throwable): Unit = {
    thr.printStackTrace()
    logger.error(s"H264RtpConsumer err message: ${thr.getMessage}")
  }


  override def onComplete(pipeline: ConsumptionPipeline[_, H264NaluData]): Unit = pipeline.unsubscribe(this)
}
