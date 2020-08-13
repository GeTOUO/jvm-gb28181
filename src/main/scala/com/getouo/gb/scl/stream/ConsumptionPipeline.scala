package com.getouo.gb.scl.stream

import java.util.concurrent.ConcurrentHashMap

import com.getouo.gb.scl.io.ISourceData
import com.getouo.gb.scl.util.Observer
import io.netty.channel.Channel

import scala.collection.concurrent
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}


trait ConsumptionPipeline[IN <: ISourceData, OUT <: ISourceData] {

  implicit val ec: ExecutionContext

  protected val consumers: concurrent.Map[String, SourceConsumer[OUT]] = new ConcurrentHashMap[String, SourceConsumer[OUT]]().asScala

  def subscribe(consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT]

  def subscribe(consumerName: String, consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT]

  def unsubscribe(consumer: SourceConsumer[OUT]): Boolean

  def unsubscribe(consumerName: String): Boolean

  def getConsumerOrElseUpdate[C <: SourceConsumer[OUT]](consumerClz: Class[C], op: => C): C = {
    val option = consumers.values.find(_.getClass == consumerClz)
    option match {
      case Some(value) => value.asInstanceOf[C]
      case None =>
        val opv = op
        subscribe(opv)
        opv
    }
  }

  def tcpTryJoin[C <: SourceConsumer[_]](clz: Class[C], channel: Channel): Unit = {
    consumers.values.filter(_.getClass == clz).foreach(f => f.tcpJoin(channel))
  }

  def udpTryJoin[C <: SourceConsumer[_]](clz: Class[C], sender: Channel, targetIpAndPort: (String, Int)): Unit = {
    consumers.values.filter(_.getClass == clz).foreach(f => f.udpJoin(sender, targetIpAndPort))
  }

  def onNext(data: IN): Unit = {
    decode(data) match {
      case Some(value) => dispatch(value)
      case None => onComplete()
    }
  }

  protected def decode(in: IN): Option[OUT]

  private def dispatch(out: OUT): Unit = {
    val iterator = consumers.values.iterator
    while (iterator.hasNext) {
      val nextConsumer = iterator.next()
      Try(nextConsumer.onNext(this, out)) match {
        case Failure(exception) => nextConsumer.onError(this, exception)
        case Success(_) =>
      }
//      Future {
//      }
    }
  }

  def onComplete(): Unit
}
