package com.tartner.vertx

import io.vertx.core.eventbus.EventBus
import io.vertx.core.logging.Logger
import io.vertx.kotlin.coroutines.CoroutineVerticle

val CoroutineVerticle.eventBus: EventBus
  get() = vertx.eventBus()

fun Logger.debugIf(messageFactory: () -> String) {
  if (isDebugEnabled) {
    debug(messageFactory.invoke())
  }
}

fun Logger.infoIf(messageFactory: () -> String) {
  if (isInfoEnabled) {
    info(messageFactory.invoke())
  }
}

