package com.getouo.gb.scl.data

trait ISourceData

trait EndSymbolData extends ISourceData

case class ErrorSourceData(thr: Throwable) extends ISourceData

case class ByteSourceData(array: Array[Byte]) extends ISourceData

trait UnsafeNaluData

case class EmptyNaluData(startLen: Int) extends UnsafeNaluData

case object EndSymbol extends EndSymbolData with H264SourceData



