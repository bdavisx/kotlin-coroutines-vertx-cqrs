package com.tartner.vertx.commands

import com.tartner.vertx.codecs.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import kotlin.reflect.*

/**
 Class that handles registering for commands in a standardized way based on the cluster node and
 command. The nodeId should be unique within the cluster.
 */
class CommandRegistrar(val nodeId: String) {
  /** Registers the command with address == commandClass.qualifiedName */
  fun <T: SerializableVertxObject> registerLocalCommandHandler(
    eventBus: EventBus, commandClass: KClass<T>, handler: Handler<Message<T>>) =
    registerCommandHandlerWithLocalAddress(eventBus, commandClass.qualifiedName!!, handler)

  /** Registers a handler for T that is only local to this node. */
  fun <T: Any> registerCommandHandlerWithLocalAddress(
    eventBus: EventBus, address: String, handler: Handler<Message<T>>) =
    eventBus.localConsumer<T>(address, { message -> handler.handle(message) })!!

  /**
   Register a cluster wide address with the address prefixed with a standardized cluster prefix
   that will be unique for each node in the cluster.
   */
  fun <T: Any> registerCommandHandlerWithClusterAddress(
    eventBus: EventBus, address: String, handler: Handler<Message<T>>) {
    val localizedAddress = "$nodeId::$address"
    eventBus.consumer<T>(localizedAddress) { message -> handler.handle(message) }
  }

  /** Registers a handler with both a local and standardized cluster prefix address. */
  fun <T: Any> registerCommandHandlerWithLocalAndClusterAddresses(
    eventBus: EventBus, address: String, handler: Handler<Message<T>>) {
    registerCommandHandlerWithLocalAndClusterAddresses(eventBus, address, handler)
    registerCommandHandlerWithClusterAddress(eventBus, address, handler)
  }
}

class EventRegistrar {
  fun <T: SerializableVertxObject>  registerEventHandler(eventBus: EventBus, eventClass: KClass<T>,
    handler: Handler<Message<T>>): MessageConsumer<T>? =
    eventBus.consumer<T>(eventClass.qualifiedName, { message -> handler.handle(message) })
}
