package com.getouo.gb.scl.stream

import com.getouo.gb.scl.io.ISourceData

trait SourceConsumer[S <: ISourceData] {

  def onNext(pipeline: ConsumptionPipeline[_, S], data: S): Unit

  def onError(pipeline: ConsumptionPipeline[_, S], thr: Throwable): Unit

  def onComplete(pipeline: ConsumptionPipeline[_, S]): Unit

}
