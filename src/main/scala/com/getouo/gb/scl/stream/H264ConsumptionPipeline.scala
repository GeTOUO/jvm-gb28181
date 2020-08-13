package com.getouo.gb.scl.stream

import com.getouo.gb.scl.io.{H264NaluData, H264SourceData}

import scala.concurrent.ExecutionContext

class H264ConsumptionPipeline(implicit val ec: ExecutionContext)
  extends DefaultConsumptionPipeline[H264SourceData, H264NaluData] {

  override def decode(in: H264SourceData): Option[H264NaluData] =
    in match {
      case out@H264NaluData(_, _) => Some(out)
      case _ => None
    }

}
