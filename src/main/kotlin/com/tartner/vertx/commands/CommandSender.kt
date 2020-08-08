package com.tartner.vertx.commands

import arrow.core.Either
import com.tartner.vertx.OpenForTesting
import com.tartner.vertx.codecs.EventBusJacksonJsonCodec
import com.tartner.vertx.codecs.SerializableVertxObject
import com.tartner.vertx.debugIf
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.coroutines.awaitResult

/**
This evolved from the CommandBus, but once I added a DI library that would work with Verticle's, I
realized that a class was a better idea. This class will leverage the Vertx EventBus better than a
CommandBus verticle would, but still has plenty of flexibility.

At some point (if necessary) this can evolve into a class that does a dynamic service lookup to get
the address, although we'll probably need to pass in another dependency or change the signature for
that to work.
 */
@OpenForTesting
class CommandSender(val eventBus: EventBus) {
  private val log = LoggerFactory.getLogger(CommandSender::class.java)

  val deliveryOptions = DeliveryOptions()
    .setCodecName(EventBusJacksonJsonCodec.codecName)
    .setSendTimeout(5000)

  fun send(command: Any) {
    log.debugIf {"Sending command $command to address ${command::class.qualifiedName}"}
    eventBus.send(command::class.qualifiedName, command, deliveryOptions)
  }

  fun <T> send(command: Any, replyHandler: Handler<AsyncResult<Message<T>>>) {
    log.debugIf {"Sending command $command to address ${command::class.qualifiedName} with reply"}
    eventBus.request(command::class.qualifiedName, command, deliveryOptions, replyHandler)
  }

  fun send(address: String, command: Any) {
    log.debugIf {"Sending command $command to $address"}
    eventBus.send(address, command, deliveryOptions)
  }

  fun <T> send(address: String, command: Any, replyHandler: Handler<AsyncResult<Message<T>>>) {
    log.debugIf {"Sending command $command to $address with reply"}
    eventBus.request(address, command, deliveryOptions, replyHandler)
  }

  fun reply(message: Message<*>, reply: Any) {
    log.debugIf {"Replying $reply to ${message.body()}"}
    message.reply(reply, deliveryOptions)
  }

  suspend fun <Failure: SerializableVertxObject, Success: SerializableVertxObject> sendA(command: Any)
    : Either<Failure, Success>
    = awaitResult<Message<Either<Failure, Success>>> { send(command, it) }.body()
}
