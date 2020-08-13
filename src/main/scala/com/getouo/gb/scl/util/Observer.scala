package com.getouo.gb.scl.util

trait Observer[IN] {
  def onNext(data: IN): Unit
}