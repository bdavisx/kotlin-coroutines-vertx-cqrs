package com.tartner.vertx.cqrs

import com.tartner.vertx.OpenForTesting
import com.tartner.vertx.codecs.EventBusJacksonJsonCodec
import com.tartner.vertx.codecs.SerializableVertxObject
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus

/** This publishes events in the "standard" way we are doing it for the library. */
@OpenForTesting
class EventPublisher(val eventBus: EventBus) {
  val deliveryOptions: DeliveryOptions = DeliveryOptions()
    .setCodecName(EventBusJacksonJsonCodec.codecName)
    .setSendTimeout(5000)

  fun publish(event: SerializableVertxObject) {
    eventBus.publish(event::class.qualifiedName, event, deliveryOptions)
  }
}
