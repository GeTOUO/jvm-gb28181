package com.getouo.gb.scl.stream

import com.getouo.gb.scl.data.{PSH264IFrame, PSH264Data}

import scala.concurrent.ExecutionContext

class GB28181ConsumptionPipeline(implicit val ec: ExecutionContext)
  extends DefaultConsumptionPipeline[PSH264Data, PSH264IFrame] {

  override def decode(in: PSH264Data): Option[PSH264IFrame] =
    in match {
      case out@PSH264IFrame(_) => Some(out)
      case _ => None
    }

}
