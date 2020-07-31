package com.tartner.vertx

import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import java.util.UUID

sealed class CodeMessage<T: Any>(val block: suspend (DirectCallVerticle) -> T)
class ReturnValueCodeMessage<T: Any>(block: suspend (DirectCallVerticle) -> T): CodeMessage<T>(block)
class UnitCodeMessage(block: suspend (DirectCallVerticle) -> Unit): CodeMessage<Unit>(block)
class FireAndForgetCodeMessage(block: suspend (DirectCallVerticle) -> Unit): CodeMessage<Unit>(block)

/**
 * Use the same id for multiple verticles if you want the calls to be distributed.
 */
open class DirectCallVerticle(val localAddress: String): CoroutineVerticle() {
  /** The noArg constructor uses a random value for the id. */
  constructor(): this(UUID.randomUUID().toString())

  companion object {
    val codeDeliveryOptions = DeliveryOptions()
    init { codeDeliveryOptions.codecName = CodeMessage::class.qualifiedName }

    fun isDirectCallVerticle(jvmType: Class<*>) =
      DirectCallVerticle::class.java.isAssignableFrom(jvmType)

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
  protected suspend fun act(block: suspend (DirectCallVerticle) -> Unit) {
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
  protected suspend fun <T: Any> actAndReply(block: suspend (DirectCallVerticle) -> T): T {
    return awaitMessageResult {
      eventBus.send(localAddress, ReturnValueCodeMessage(block), codeDeliveryOptions, it)
    }
  }

  /**
   * Code inside `block` will run on the event loop for this (set of) verticle(s). The calling
   * thread returns immediately.
   */
  protected fun fireAndForget(block: suspend (DirectCallVerticle) -> Unit) {
    eventBus.send(localAddress, FireAndForgetCodeMessage(block), codeDeliveryOptions)
  }

  private suspend fun runCode(codeMessage: Message<CodeMessage<*>>) {
      // TODO: exceptions?
    val code = codeMessage.body()!!
    when (codeMessage.body()) {
      is UnitCodeMessage -> {
        code.block.invoke(this)
        codeMessage.reply(1)
      }
      is FireAndForgetCodeMessage -> {
        code.block.invoke(this)
      }
      is ReturnValueCodeMessage -> {
        codeMessage.reply(code.block.invoke(this))
      }
    }
  }
}
