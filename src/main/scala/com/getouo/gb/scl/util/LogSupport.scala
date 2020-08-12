package com.getouo.gb.scl.util

import io.netty.util.internal.logging.{InternalLogger, InternalLoggerFactory}

trait LogSupport {
  protected val logger: InternalLogger = InternalLoggerFactory.getInstance(this.getClass)
}
