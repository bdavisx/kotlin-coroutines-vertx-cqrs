package com.tartner.vertx.commands

import com.tartner.vertx.*
import com.tartner.vertx.codecs.*
import io.vertx.core.*
import io.vertx.core.eventbus.*

/**
 This evolved from the CommandBus, but once I added a DI library that would work with Verticle's, I
 realized that a class was a better idea. This class will leverage the Vertx EventBus better than a
 CommandBus verticle would, but still has plenty of flexibility.

 At some point this can evolve into a class that does a dynamic service lookup to get the address,
 although we'll probably need to pass in another dependency or change the signature for that to
 work.
 */
@OpenForTesting
class CommandSender {
  val deliveryOptions: DeliveryOptions = DeliveryOptions()
    .setCodecName(EventBusSerializationCodec.codecName)
    .setSendTimeout(5000)

  fun send(eventBus: EventBus, command: SerializableVertxObject) {
    eventBus.send(command::class.qualifiedName, command, deliveryOptions)
  }

  fun <T> send(eventBus: EventBus, command: SerializableVertxObject,
    replyHandler: Handler<AsyncResult<Message<T>>>) {

    eventBus.send(command::class.qualifiedName, command, deliveryOptions, replyHandler)
  }

  fun send(eventBus: EventBus, address: String, command: SerializableVertxObject) {
    eventBus.send(address, command, deliveryOptions)
  }

  fun <T> send(eventBus: EventBus, address: String, command: SerializableVertxObject,
    replyHandler: Handler<AsyncResult<Message<T>>>) {
    eventBus.send(address, command, deliveryOptions, replyHandler)
  }

  fun reply(message: Message<*>, reply: Any) {
    message.reply(reply, deliveryOptions)
  }
}
