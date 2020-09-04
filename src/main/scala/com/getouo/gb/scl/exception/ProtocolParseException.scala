package com.getouo.gb.scl.exception

class ProtocolParseException(message: String, cause: Exception) extends RuntimeException(message, cause) {

  private var errorOffset = 0

  def this(errorOffset: Int, message: String) {
    this(message, null)
    this.errorOffset = errorOffset
  }

  def this(message: String) {
    this(0, message)
  }

  def this(errorOffset: Int, message: String, cause: Exception) {
    this(message, cause)
    this.errorOffset = errorOffset
  }

  def getErrorOffset: Int = this.errorOffset

}
