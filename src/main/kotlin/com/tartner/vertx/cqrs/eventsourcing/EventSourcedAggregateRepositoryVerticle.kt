package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.Either
import com.tartner.vertx.awaitMessageEitherResult
import com.tartner.vertx.commands.CommandFailedDueToException
import com.tartner.vertx.commands.CommandFailureDueToException
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.cqrs.AggregateEvent
import com.tartner.vertx.cqrs.AggregateId
import com.tartner.vertx.cqrs.AggregateSnapshot
import com.tartner.vertx.cqrs.DefaultDomainCommand
import com.tartner.vertx.cqrs.DomainCommand
import com.tartner.vertx.cqrs.HasAggregateVersion
import com.tartner.vertx.cqrs.successReplyRight
import com.tartner.vertx.eventBus
import com.tartner.vertx.functional.createLeft
import com.tartner.vertx.functional.createRight
import com.tartner.vertx.functional.flatMapS
import com.tartner.vertx.functional.mapS
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.shareddata.Lock
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import kotlin.reflect.KClass

typealias AggregateVerticleFactory = (AggregateId) -> CoroutineVerticle

class UnableToFindAggregateFactoryException(val snapshotOrEvent: HasAggregateVersion):
  RuntimeException()

/** I don't think this class can be sent across the wire. */
data class RegisterInstantiationClassesForAggregateLocalCommand(
  val factory: AggregateVerticleFactory,
  val eventClasses: List<KClass<out AggregateEvent>>,
  val snapshotClasses: List<KClass<out AggregateSnapshot>>)
  : DomainCommand by DefaultDomainCommand()

data class LoadEventSourcedAggregateCommand(
  val aggregateId: AggregateId, val aggregateAddress: String):
  DomainCommand by DefaultDomainCommand()

data class LoadEventSourcedAggregateCommandFailure(override val cause: Throwable)
  : CommandFailureDueToException

internal object SharedEventSourcedAggregateRepositorySnapshotQuery
  : DomainCommand by DefaultDomainCommand()

// TODO: See GeneralCqrsDesign.md for documentation on the class

/**
 If it's the repository, it should handle the "caching", not the command handler.
 Only meant to be called locally.
 */
class EventSourcedAggregateRepositoryVerticle(
  private val sharedRepositoryData: SharedEventSourcedAggregateRepositoryData,
  private val dataVerticle: EventSourcedAggregateDataVerticle,
  private val commandSender: CommandSender,
  private val commandRegistrar: CommandRegistrar
): CoroutineVerticle() {

  override suspend fun start() {
    commandRegistrar.registerCommandHandler(
      this, LoadEventSourcedAggregateCommand::class, ::loadAggregate)

    commandRegistrar.registerLocalCommandHandler(
      eventBus, RegisterInstantiationClassesForAggregateLocalCommand::class,
      Handler { registerInstantiationClasses(it) })

    commandRegistrar.registerLocalCommandHandler(
      eventBus, SharedEventSourcedAggregateRepositorySnapshotQuery::class,
      Handler { debugSharedData(it) })
  }

  private fun registerInstantiationClasses(
    commandMessage: Message<RegisterInstantiationClassesForAggregateLocalCommand>) {
    val command = commandMessage.body()

    command.eventClasses.forEach {
      sharedRepositoryData.addInstantiationClass(it, command.factory)
    }
    command.snapshotClasses.forEach {
      sharedRepositoryData.addInstantiationClass(it, command.factory)
    }
    commandSender.reply(commandMessage, successReplyRight)
  }

  private suspend fun loadAggregate(commandMessage: Message<LoadEventSourcedAggregateCommand>) {
    val command = commandMessage.body()
    val aggregateId = command.aggregateId

    if (sharedRepositoryData.isAggregateDeployed(aggregateId)) {
      successfulLoad(aggregateId, commandMessage)
    } else {
      val sharedData = vertx.sharedData()
      val lock = awaitResult<Lock> { sharedData.getLock(command.aggregateAddress, it) }

      try {
        if (!sharedRepositoryData.isAggregateDeployed(aggregateId)) {
          loadAggregateFromStorage(aggregateId, command)
        }

        successfulLoad(aggregateId, commandMessage)
      } catch (ex: Throwable) {   // may want to separate out EitherFailureException??
        commandSender.reply(commandMessage, CommandFailedDueToException(ex).createLeft())
      } finally {
        lock.release()
      }
    }
  }

  private fun successfulLoad(aggregateId: AggregateId,
    commandMessage: Message<LoadEventSourcedAggregateCommand>) {
    sharedRepositoryData.markAggregateRecentlyUsed(aggregateId)
    commandSender.reply(commandMessage, createRight())
  }

  private suspend fun loadAggregateFromStorage(
    aggregateId: AggregateId, command: LoadEventSourcedAggregateCommand)
    : Either<CommandFailedDueToException, Unit> {

    val snapshotEither: Either<CommandFailedDueToException, AggregateSnapshot?> =
      dataVerticle.loadLatestAggregateSnapshot(aggregateId)

    return snapshotEither.flatMapS { possibleSnapshot ->
      // if snapshot then get it's version + 1, otherwise use 0 (-1 + 1)
      val aggregateVersion = (possibleSnapshot?.aggregateVersion ?: -1) + 1

      val eventsEither: Either<CommandFailedDueToException, List<AggregateEvent>> =
        dataVerticle.loadAggregateEvents(aggregateId, aggregateVersion)

      eventsEither.mapS { events ->
        val snapshotOrEvent: HasAggregateVersion = possibleSnapshot ?: events.first()

        val verticleFactory: AggregateVerticleFactory? = sharedRepositoryData.findFactory(
          snapshotOrEvent::class)

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

        sharedRepositoryData.addAggregateDeployment(aggregateId, deploymentId)

        // TODO: need to handle this in Event delegate - or the event delegate needs to register
        // the aggregate to handle the command
        val eventsApplication = awaitMessageEitherResult<Any> {
          commandSender.send(eventBus, command.aggregateAddress,
            ApplySnapshotAndEventsFromLoadAggregateCommand(aggregateId, possibleSnapshot, events), it)
        }
      }
    }
  }

  private fun debugSharedData(
    commandMessage: Message<SharedEventSourcedAggregateRepositorySnapshotQuery>) {
    commandSender.reply(commandMessage, sharedRepositoryData.snapshot().createRight())
  }
}
