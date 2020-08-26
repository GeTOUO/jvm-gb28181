package com.getouo.gb.scl.stream

import com.getouo.gb.scl.data.PSH264Data

import scala.concurrent.ExecutionContext

class GB28181ConsumptionPipeline(implicit val ec: ExecutionContext)
  extends DefaultConsumptionPipeline[PSH264Data, PSH264Data] {

  override def decode(in: PSH264Data): Option[PSH264Data] =
    Option.apply(in)

}
