package com.getouo.gb.scl.stream

import java.util.concurrent.ConcurrentHashMap

import com.getouo.gb.scl.io.{H264FileSource, H264NaluData, H264SourceData, ISourceData, Source}

import scala.collection.concurrent

class PlayStream[IN <: ISourceData, OUT <: ISourceData](source: Source[IN], consumptionPipeline: ConsumptionPipeline[IN, OUT]) {

  def subscribe(consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT] = consumptionPipeline.subscribe(consumer)

  def subscribe(consumerName: String, consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT] = consumptionPipeline.subscribe(consumerName, consumer)

//  def unsubscribe(consumer: SourceConsumer[OUT]): Boolean

  //
  //  def unsubscribe(consumerName: String): Boolean
}

class H264PlayStream(source: H264FileSource, pipeline: H264ConsumptionPipeline) extends PlayStream[H264SourceData, H264NaluData](source, pipeline) {

}

object PlayStream {
  val groups: concurrent.Map[String, SourceConsumer[OUT]] = new ConcurrentHashMap[String, SourceConsumer[OUT]]().asScala
}