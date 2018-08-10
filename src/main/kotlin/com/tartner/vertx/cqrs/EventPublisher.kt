package com.tartner.vertx.cqrs

import com.tartner.vertx.*
import com.tartner.vertx.codecs.*
import io.vertx.core.eventbus.*

/** This publishes events in the "standard" way we are doing it for the library. */
class EventPublisher(val eventBus: EventBus) {
  val deliveryOptions: DeliveryOptions = DeliveryOptions()
    .setCodecName(EventBusJacksonJsonCodec.codecName)
    .setSendTimeout(5000)

  fun publish(event: SerializableVertxObject) {
    eventBus.publish(event::class.qualifiedName, event, deliveryOptions)
  }
}
