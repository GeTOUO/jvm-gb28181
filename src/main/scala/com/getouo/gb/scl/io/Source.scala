package com.getouo.gb.scl.io

import com.getouo.gb.scl.util.Observer

import scala.util.Try

/**
 * 资源
 *
 * @tparam S
 */
sealed trait Source[S <: ISourceData] {
  def load(): Unit
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

  private var obs: Observer[S] = null
  def registerObserver(ob: Observer[S]): Unit = obs = ob

  protected final def onProduced(source: S): Unit =
    if (obs != null) Try(obs.onNext(source))

}
