package com.getouo.gb.scl.util

import scala.util.{Failure, Success, Try}

object RtpUrlUtil {
  private val urlR = "rtsp://[^/]*/(.*)".r
  def getSourceName(url: String): String = {
    Try {
      val urlR(sn) = url
      sn
    } match {
      case Failure(exception) => url
      case Success(value) => value
    }
  }

}
