package com.getouo.gb.scl.stream

import com.getouo.gb.scl.io.{GB28181H264DataData, GB28181SourceData}

import scala.concurrent.ExecutionContext

class GB28181ConsumptionPipeline(implicit val ec: ExecutionContext)
  extends DefaultConsumptionPipeline[GB28181SourceData, GB28181H264DataData] {

  override def decode(in: GB28181SourceData): Option[GB28181H264DataData] =
    in match {
      case out@GB28181H264DataData(_) => Some(out)
      case _ => None
    }

}
