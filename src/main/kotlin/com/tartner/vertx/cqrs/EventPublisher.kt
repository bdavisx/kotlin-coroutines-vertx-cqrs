package com.tartner.vertx.cqrs

import com.tartner.vertx.codecs.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import kotlin.reflect.*

/** This publishes events in the "standard" way we are doing it for the library. */
class EventPublisher(val eventBus: EventBus) {
  val deliveryOptions: DeliveryOptions = DeliveryOptions()
    .setCodecName(EventBusJacksonJsonCodec.codecName)
    .setSendTimeout(5000)   // TODO: sendTimeout needs to be configurable

  fun publish(event: SerializableVertxObject) {
    eventBus.publish(event::class.qualifiedName, event, deliveryOptions)
  }
}

class EventRegistrar {
  fun <T: SerializableVertxObject>  registerEventHandler(eventBus: EventBus, eventClass: KClass<T>,
    handler: Handler<Message<T>>): MessageConsumer<T>? =
    eventBus.consumer<T>(eventClass.qualifiedName, { message -> handler.handle(message) })
}
