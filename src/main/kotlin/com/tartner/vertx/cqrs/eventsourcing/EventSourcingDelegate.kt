package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.tartner.utilities.*
import com.tartner.vertx.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.functional.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*

/**
 * Many functions will return an Either<FailureReply, *>, this is the standard to return if version
 * creation fails.
 */
data class CreateAggregateVersionFailed(val errorEvent: ErrorEvent): FailureReply

/**
 NOTE: Each Aggregate/Verticle needs their own copy of this.

 Responsible for handling the Event Sourcing aspects for a delegate.
 */
class EventSourcingDelegate(
  private val aggregateId: AggregateId,
  private val vertx: Vertx,
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender,
  private val eventBus: EventBus,
  private val eventPublisher: EventPublisher
) {
  companion object {
    const val initialVersion = 0L
  }

  private val commandConsumers = mutableListOf<MessageConsumer<out DomainCommand>>()
  private var version: Long = initialVersion
  val aggregateAddress: String = determineAggregateAddress(aggregateId)

  private fun <T: DomainCommand> addMessageConsumer(consumer: MessageConsumer<T>) {
    commandConsumers.add(consumer)
  }

  /**
  The address to call the aggregate with a direct command message.

  directMessageHandler: suspend (Message<DomainCommand>) -> Unit
   */
  private fun determineAggregateAddress(aggregateId: AggregateId) = aggregateId.toStringFast()

  /**
  Handles registering the aggregate in a standard fashion.

  The directMessageHandler will be called whenever a message comes to the aggregate's registered
  address.

  // TODO: make this an interface so networking can be handled with a different class?

  have a way to create networked addresses and register globally
   */
  fun registerAggregate(directMessageHandler: suspend (Message<DomainCommand>) -> Unit) {
    // handles anything sent directly to our Id as a String address
    addMessageConsumer(commandRegistrar.registerCommandHandlerWithLocalAddress<DomainCommand>(
      eventBus, determineAggregateAddress(aggregateId), {directMessageHandler(it)}))
  }

  fun firstVersion(command: DomainCommand): Either<ErrorEvent, Long> =
    if (version == initialVersion) {
      version++
      version.createRight()
    }
    else {
      AttemptToInitializeAlreadyInitializedAggregateEvent(
        aggregateId, command, command.correlationId).createLeft()
    }

  fun nextVersion(command: DomainCommand): Either<ErrorEvent, Long> =
    if (version != initialVersion) {
      version++
      version.createRight()
    }
    else {
      AttemptToSendCommandToUninitializedAggregateEvent(aggregateId, command, command.correlationId)
        .createLeft()
    }

  fun setVersionFromSnapshot(snapshot: AggregateSnapshot) {
    this.version = snapshot.aggregateVersion
  }

  /** Note this does not persist the event, only publishes and replies to the message with it. */
  fun fail(commandMessage: Message<*>, failEvent: ErrorEvent): ErrorEvent {
    eventPublisher.publish(failEvent)
    commandSender.reply(commandMessage, Either.Left(failEvent))
    return failEvent
  }

  /**
  If Either.left<FailureReply> is returned, the aggregate should mark itself invalid and reply
  to any sent commands with an Either.left<AggregateInvalidatedEvent> failure reply. In g

  TODO: Needs a new name.
   */
  suspend fun storeAndPublishEvents(events: List<AggregateEvent>, eventBus: EventBus):
    Either<FailureReply, SuccessReply> {

    val storeResult: Either<FailureReply, SuccessReply> = awaitMessageEitherResult {
      commandSender.send(eventBus, StoreAggregateEventsCommand(aggregateId, events), it) }

    return storeResult.foldS({
      // TODO: error message
      UnableToStoreAggregateEventsCommandFailure(
        "UnableToStoreAggregateEventsCommandFailure - aggregate will be invalidated - id : $aggregateId",
        aggregateId, events, storeResult).createLeft()
    }, {
      events.forEach { eventPublisher.publish(it) }
      successReplyRight
    })
  /**
  should events be published if they weren't saved? They've been applied to the parent, BUT, the
  parent can be reset to the saved events. The thing is though, it's not the parent that should be
  dealing with the reset, so a better solution would be to throw an UnableToSaveEventsErrorEvent
  to the eventBus and have the system handle it. But I don't think the events s/b published at
  that point and the aggregate should be unloaded because it's in an invalid state.

  The problem with the above idea is that there could already be commands in the aggregate's
  mailbox, and they would be processed by the invalid aggregate. So the aggregate *has* to be
  notified that it's invalid *RIGHT NOW*. After that, a UnableToSaveEventsErrorEvent or something
  similar could be put on the bus.

  TODO: document that the error/either needs to be handled wherever it is affecting, iow since we
  can't save our events, we're in bad shape and need to mark the containing aggregate as bad
  (there's code around that already). But perhaps it needs to be a callback from here, a generic
  invalidateAggregate callback that this delegate can call @ any time, that way we don't have to
  wait for a message to get passed, because there could already be commands in the queue, and this
  aggregate needs to be invalidated immediately.

  Then, the failure would get propagated back to the caller @ some point so an error message could
  be displayed.
   */
  }


  fun applySnapshot(snapshot: AggregateSnapshot) {
    version = snapshot.aggregateVersion
  }
}

