package com.getouo.gb.scl.exception

class ProtocolUnSupportException(message: String, cause: Exception) extends RuntimeException(message, cause) {

  def this(message: String) {
    this(message, null)
  }

}
