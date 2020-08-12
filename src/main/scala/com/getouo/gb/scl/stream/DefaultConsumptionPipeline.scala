package com.getouo.gb.scl.stream

import java.util.concurrent.ConcurrentHashMap

import com.getouo.gb.scl.io.ISourceData

import scala.collection._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

abstract class DefaultConsumptionPipeline[IN <: ISourceData, OUT <: ISourceData](implicit ec: ExecutionContext) extends ConsumptionPipeline[IN, OUT] {

  private val consumers: concurrent.Map[String, SourceConsumer[OUT]] = new ConcurrentHashMap[String, SourceConsumer[OUT]]().asScala

  override def subscribe(consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT] = subscribe(consumer.getClass.getName, consumer)

  override def subscribe(consumerName: String, consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT] = {
    consumers.put(consumerName, consumer);
    this
  }

  override def unsubscribe(consumer: SourceConsumer[OUT]): Boolean = unsubscribe(consumer.getClass.getName)

  override def unsubscribe(consumerName: String): Boolean = consumers.remove(consumerName).isDefined

  private def dispatch(out: OUT): Unit = {
    val iterator = consumers.values.iterator
    while (iterator.hasNext) {
      Future {
        val nextConsumer = iterator.next()
        Try(nextConsumer.onNext(this, out)) match {
          case Failure(exception) => nextConsumer.onError(this, exception)
          case Success(_) =>
        }
      }
    }
  }
  def onNext(data: IN): Unit = {
    decode(data) match {
      case Some(value) => dispatch(value)
      case None => onComplete()
    }

  }

  def decode(in: IN): Option[OUT]

  def onComplete(): Unit = {
    val iterator = consumers.values.iterator
    while (iterator.hasNext) {
      Future {
        val nextConsumer = iterator.next()
        Try(nextConsumer.onComplete(this)) match {
          case Failure(exception) => nextConsumer.onError(this, exception)
          case Success(_) =>
        }
      }
    }
  }
}

