package com.getouo.gb.scl.io

/**
 * 资源
 * @tparam S
 */
sealed trait Source[S <: ISourceData] {
  def produce(): S
}

/**
 * 被动生产的资源
 * @tparam S
 */
trait UnActiveSource[S <: ISourceData] extends Source[S] {
  def produce(): S
}

/**
 * 主动生产的资源
 * @tparam S
 */
trait ActiveSource[S <: ISourceData] extends Source[S] {
  def onProduced(source: S): Unit
}
