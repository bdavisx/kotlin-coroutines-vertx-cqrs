package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.codecs.SerializableVertxObject
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.cqrs.AggregateCommand
import com.tartner.vertx.cqrs.AggregateId
import com.tartner.vertx.eventBus
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.shareddata.LocalMap
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch

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
