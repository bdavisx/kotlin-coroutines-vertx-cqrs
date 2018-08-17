package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.functional.*
import io.vertx.core.eventbus.*
import io.vertx.core.logging.*
import io.vertx.core.shareddata.*
import io.vertx.kotlin.coroutines.*

typealias AggregateVerticleFactory = (AggregateId) -> CoroutineVerticle

class UnableToFindAggregateFactoryException(val snapshotOrEvent: HasAggregateVersion):
  RuntimeException()

data class LoadEventSourcedAggregateCommand(
  val aggregateId: AggregateId, val aggregateAddress: String,
  override val correlationId: CorrelationId = newId()
): DomainCommand

data class LoadEventSourcedAggregateCommandFailure(override val cause: Throwable,
  override val correlationId: CorrelationId)
  : CommandFailureDueToException

// TODO: See GeneralCqrsDesign.md for documentation on the class

/**
 If it's the repository, it should handle the "caching", not the command handler.
 Only meant to be called locally.
 */
class EventSourcedAggregateRepositoryVerticle(
  private val commandSender: CommandSender,
  private val commandRegistrar: CommandRegistrar
): CoroutineVerticle() {
  private val log = LoggerFactory.getLogger(EventSourcedAggregateRepositoryVerticle::class.java)

  override suspend fun start() {
    commandRegistrar.registerLocalCommandHandler( eventBus, LoadEventSourcedAggregateCommand::class,
      { loadAggregate(it) })
  }

  private suspend fun loadAggregate(commandMessage: Message<LoadEventSourcedAggregateCommand>) {
    val command = commandMessage.body()
    val aggregateId = command.aggregateId

    if (isAggregateDeployed(aggregateId, command.correlationId)) {
      successfulLoad(aggregateId, commandMessage)
    } else {
      val sharedData = vertx.sharedData()
      val lock = awaitResult<Lock> { sharedData.getLock(command.aggregateAddress, it) }

      try {
        if (!isAggregateDeployed(aggregateId, command.correlationId)) {
          loadAggregateFromStorage(aggregateId, command)
        }

        successfulLoad(aggregateId, commandMessage)
      } catch (ex: Throwable) {   // may want to separate out EitherFailureException??
        commandSender.reply(commandMessage,
          CommandFailedDueToException(ex, command.correlationId).createLeft())
      } finally {
        lock.release()
      }
    }
  }

  suspend fun isAggregateDeployed(aggregateId: AggregateId, correlationId: CorrelationId) =
    awaitMessageResult<IsAggregateDeployedQueryReply> {
      commandSender.send(eventBus, IsAggregateDeployedQuery(aggregateId, correlationId), it)
    }.isDeployed

  private fun successfulLoad(aggregateId: AggregateId,
    commandMessage: Message<LoadEventSourcedAggregateCommand>) {
    val command = commandMessage.body()
    commandSender.send(eventBus, MarkAggregateRecentlyUsedCommand(aggregateId, command.correlationId))
    commandSender.reply(commandMessage, createRight())
  }

  private suspend fun loadAggregateFromStorage(
    aggregateId: AggregateId, command: LoadEventSourcedAggregateCommand) {

    val possibleSnapshot = awaitMessageEitherResult<AggregateSnapshot?> {
      commandSender.send(eventBus, LoadLatestAggregateSnapshotCommand(aggregateId), it)
    }

    // if snapshot then get it's version + 1, otherwise use 0 (-1 + 1)
    val aggregateVersion = (possibleSnapshot?.aggregateVersion ?: -1) + 1

    val events: List<AggregateEvent> = awaitMessageEitherResult {
      commandSender.send(eventBus, LoadAggregateEventsCommand(aggregateId, aggregateVersion), it)
    }
    val snapshotOrEvent: HasAggregateVersion = possibleSnapshot ?: events.first()

    val verticleFactory: AggregateVerticleFactory? =
      awaitMessageResult<FindAggregateVerticleFactoryReply> { commandSender.send(eventBus,
        FindAggregateVerticleFactoryQuery(snapshotOrEvent::class, command.correlationId), it) }
        .factory

    // TODO: better error handling/reporting here
    val deploymentId = when (verticleFactory) {
      null -> throw UnableToFindAggregateFactoryException(snapshotOrEvent)
      else -> {
        awaitResult<String> {
          val verticle = verticleFactory.invoke(snapshotOrEvent.aggregateId)
          vertx.deployVerticle(verticle, it)
        }
      }
    }

    commandSender.send(eventBus,
      AddAggregateDeploymentCommand(aggregateId, deploymentId, command.correlationId))

    // TODO: need to handle this in Event delegate - or the event delegate needs to register
    // the aggregate to handle the command
    val eventsApplication = awaitMessageEitherResult<Any> {
      commandSender.send(eventBus, command.aggregateAddress,
        ApplySnapshotAndEventsFromLoadAggregateCommand(aggregateId, possibleSnapshot, events), it)
    }
  }
}
