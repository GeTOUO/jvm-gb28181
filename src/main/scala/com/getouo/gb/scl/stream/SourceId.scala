package com.getouo.gb.scl.stream

trait SourceId {}

case class FileSourceId(file: String, setupTime: Long) extends SourceId

case class GBSourceId(file: String, setupTime: Long) extends SourceId
