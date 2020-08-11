package com.getouo.gb

import scala.util.matching.Regex

package object scl {

  private val portAccessor: Regex = ".*client_port=([0-9]+)-.*".r

  def extractClientTransport(transport: String): Int = {
    val portAccessor(port) = transport
    port.toInt
  }

}
