package com.tartner.vertx

import io.vertx.core.eventbus.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import java.util.*

sealed class CodeMessage<T: Any>(val block: suspend () -> T)
class ReturnValueCodeMessage<T: Any>(block: suspend () -> T): CodeMessage<T>(block)
class UnitCodeMessage(block: suspend () -> Unit): CodeMessage<Unit>(block)
class FireAndForgetCodeMessage(block: suspend () -> Unit): CodeMessage<Unit>(block)

/**
 * Use the same id for multiple verticles if you want the calls to be distributed.
 */
open class DirectCallVerticle(private val localAddress: String): CoroutineVerticle() {
  constructor(): this(UUID.randomUUID().toString())

  companion object {
    val codeDeliveryOptions = DeliveryOptions()
    init { codeDeliveryOptions.codecName = CodeMessage::class.qualifiedName }
  }

  override suspend fun start() {
    super.start()
    vertx.eventBus().localConsumer<CodeMessage<*>>(localAddress,
      { launch(vertx.dispatcher()) { runCode(it) } })
  }

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The code runs
   * like coroutines are expected to run, the calling thread awaits on the return, it does not "fire
   * and forget".
   */
  protected suspend fun act(block: suspend () -> Unit) {
    // we could fire and forget here, but we want the semantics of "imperative" code like coroutines have
    awaitMessageResult<Any> {
      eventBus.send(localAddress, UnitCodeMessage(block), codeDeliveryOptions, it)
    }
  }

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The code runs
   * like coroutines are expected to run, the calling thread awaits on the return, it does not "fire
   * and forget". The value returned by the block will be the return value for this function.
   */
  protected suspend fun <T: Any> actAndReply(block: suspend () -> T): T {
    return awaitMessageResult {
      eventBus.send(localAddress, ReturnValueCodeMessage(block), codeDeliveryOptions, it)
    }
  }

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The calling
   * thread returns immediately.
   */
  protected fun fireAndForget(block: suspend () -> Unit) {
    eventBus.send(localAddress, FireAndForgetCodeMessage(block), codeDeliveryOptions)
  }

  private suspend fun runCode(codeMessage: Message<CodeMessage<*>>) {
      // TODO: exceptions?
    val code = codeMessage.body()!!
    when (codeMessage.body()) {
      is UnitCodeMessage -> {
        code.block.invoke()
        codeMessage.reply(1)
      }
      is FireAndForgetCodeMessage -> {
        code.block.invoke()
      }
      is ReturnValueCodeMessage -> {
        codeMessage.reply(code.block.invoke())
      }
    }
  }
}
