package com.getouo.gb.scl.rtp

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

import com.getouo.gb.scl.io.H264NaluData
import com.getouo.gb.scl.stream.{ConsumptionPipeline, SourceConsumer}
import com.getouo.gb.scl.util.LogSupport
import io.netty.buffer.Unpooled
import io.netty.channel.socket.DatagramPacket
import io.netty.util.concurrent.Future

class H264RtpConsumer extends SourceConsumer[H264NaluData] with LogSupport {

  private val sendSeq: AtomicInteger = new AtomicInteger(0)
  private var timestamp: Int = 0
  private val framerate: Float = 25
  private val timestampIncrement: Int = (90000 / framerate).intValue() //+0.5


  var count = 0
  override def onNext(pipeline: ConsumptionPipeline[_, H264NaluData], data: H264NaluData): Unit = {
    timestamp += timestampIncrement
      count+=1
    data.rtpPacket(sendSeq, timestamp).map(Unpooled.copiedBuffer).foreach(bf => {
      logger.info(s"H264RtpConsumer send count = ${count}")
      udpSubscriber.foreach { case (channel, subscribers) =>
        subscribers.map(info => {
          bf.retain(); new DatagramPacket(bf, new InetSocketAddress(info._1, info._2))
        }).foreach(addr => {
          val future = channel.writeAndFlush(addr)
          if (count >= 1490) {
            future.addListener((f: Future[_]) => {
              logger.warn(s"the count ${count} send result= ${f.isSuccess}")
            })
          }
        })
      }
      tcpSubscriber.writeAndFlush(bf)
    })
  }

  override def onError(pipeline: ConsumptionPipeline[_, H264NaluData], thr: Throwable): Unit = {
    thr.printStackTrace()
    logger.error(s"H264RtpConsumer err message: ${thr.getMessage}")
  }


  override def onComplete(pipeline: ConsumptionPipeline[_, H264NaluData]): Unit = pipeline.unsubscribe(this)
}
