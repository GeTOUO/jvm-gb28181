package com.getouo.gb.scl.server

import io.netty.util.internal.logging.{InternalLogger, InternalLoggerFactory}
import javax.annotation.PostConstruct


trait RunnableServer extends Runnable {

  protected val logger: InternalLogger = InternalLoggerFactory.getInstance(this.getClass)

  @PostConstruct
  final def init(): Unit = {
    val thread: Thread = new Thread(this)
    thread.setDaemon(true)

    thread.setName(s" ${getClass.getSimpleName} thread")
    thread.start()
  }
}
