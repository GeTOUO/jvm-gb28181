package com.getouo.gb.scl.stream

import com.getouo.gb.scl.io.ISourceData

trait ConsumptionPipeline[IN <: ISourceData, OUT <: ISourceData] {

  def subscribe(consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT]

  def subscribe(consumerName: String, consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT]

  def unsubscribe(consumer: SourceConsumer[OUT]): Boolean

  def unsubscribe(consumerName: String): Boolean
}
