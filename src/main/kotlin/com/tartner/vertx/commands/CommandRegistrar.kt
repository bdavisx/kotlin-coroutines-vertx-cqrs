package com.tartner.vertx.commands

import com.tartner.vertx.codecs.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import kotlin.reflect.*

typealias MessageHandler<T> = suspend (Message<T>) -> Unit

/**
 Class that handles registering for commands in a standardized way based on the cluster node and
 command. The nodeId should be unique within the cluster.
 */
class CommandRegistrar(val nodeId: String, private val vertx: Vertx) {
  /** Registers the command with address == commandClass.qualifiedName */
  fun <T: SerializableVertxObject> registerLocalCommandHandler(
    eventBus: EventBus, commandClass: KClass<T>, handler: MessageHandler<T>) =
    registerCommandHandlerWithLocalAddress(eventBus, commandClass.qualifiedName!!, handler)

  /** Registers a handler for T that is only local to this node. */
  fun <T: Any> registerCommandHandlerWithLocalAddress(
    eventBus: EventBus, address: String, handler: MessageHandler<T>) =
    eventBus.localConsumer<T>(address, { launch(vertx.dispatcher()) {handler(it)} })!!

  /**
   Register a cluster wide address with the address prefixed with a standardized cluster prefix
   that will be unique for each node in the cluster.
   */
  fun <T: Any> registerCommandHandlerWithClusterAddress(
    eventBus: EventBus, address: String, handler: MessageHandler<T>) {
    val localizedAddress = "$nodeId::$address"
    eventBus.consumer<T>(localizedAddress) { launch(vertx.dispatcher()) {handler(it)} }
  }

  /** Registers a handler with both a local and standardized cluster prefix address. */
  fun <T: Any> registerCommandHandlerWithLocalAndClusterAddresses(
    eventBus: EventBus, address: String, handler: MessageHandler<T>) {
    registerCommandHandlerWithLocalAndClusterAddresses(eventBus, address, handler)
    registerCommandHandlerWithClusterAddress(eventBus, address, handler)
  }
}

