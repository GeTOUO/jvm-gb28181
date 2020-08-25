package com.getouo.gb.scl.stream

import com.getouo.gb.scl.data.ISourceData

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract class DefaultConsumptionPipeline[IN <: ISourceData, OUT <: ISourceData](implicit ec: ExecutionContext) extends ConsumptionPipeline[IN, OUT] {

  override def subscribe(consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT] = subscribe(consumer.getClass.getName, consumer)

  override def subscribe(consumerName: String, consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT] = {
    consumers.put(consumerName, consumer)
    this
  }

  override def unsubscribe(consumer: SourceConsumer[OUT]): Boolean = unsubscribe(consumer.getClass.getName)

  override def unsubscribe(consumerName: String): Boolean = consumers.remove(consumerName).isDefined

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

