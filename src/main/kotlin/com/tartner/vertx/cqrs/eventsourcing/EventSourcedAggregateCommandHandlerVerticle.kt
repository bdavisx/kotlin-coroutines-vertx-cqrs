package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.core.shareddata.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*

/**
 This class "directs" the ES Aggregate commands to the correct aggregate. That's its only
 responsibility, the repository is tasked with loading/"caching" the aggregates.
 */
class EventSourcedAggregateCommandHandlerVerticle(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender,
  private val aggregateAddresses: AggregateAddressStrategy
): CoroutineVerticle() {
  // TODO: at some point, add a way to send a message globally that clears caches and schemes and
  // reloads
  companion object {
    private const val aggregateIdToAggregateAddressMapName =
      "EventSourcedAggregate.aggregateIdToAggregateAddressMapName"
  }

  private lateinit var aggregateIdToAddress: LocalMap<AggregateId, String>

  override suspend fun start() {
    val sharedData = vertx.sharedData()
    aggregateIdToAddress =
      sharedData.getLocalMap<AggregateId, String>(aggregateIdToAggregateAddressMapName)

    commandRegistrar.registerCommandHandlerWithLocalAndClusterAddresses(
      eventBus, AggregateCommandAddress,
      Handler<Message<AggregateCommand>> {
        launch(vertx.dispatcher()) { routeCommand(it) }
      })
  }

  private suspend fun routeCommand(commandMessage: Message<AggregateCommand>) {
    val command = commandMessage.body()
    val aggregateId = command.aggregateId

    val aggregateAddress = if (aggregateIdToAddress.containsKey(aggregateId)) {
      aggregateIdToAddress[aggregateId]!!
    } else {
      val address = aggregateAddresses.determineAggregateAddress(aggregateId)
      aggregateIdToAddress[aggregateId] = address
      address
    }

    if (aggregateAddresses.isAggregateLocal(aggregateId)) {
      val result = awaitResult { it: Handler<AsyncResult<Message<SerializableVertxObject>>> ->
        commandSender.send(eventBus,
          LoadEventSourcedAggregateCommand(aggregateId, aggregateAddress), it)
      }
      // TODO: failure handling???
    }

    // TODO: we need to determine the remote command handlers address
    val result = awaitResult { it: Handler<AsyncResult<Message<SerializableVertxObject>>> ->
      commandSender.send(eventBus, aggregateAddress, command, it)
    }

    commandSender.reply(commandMessage, result.body())
  }
}
