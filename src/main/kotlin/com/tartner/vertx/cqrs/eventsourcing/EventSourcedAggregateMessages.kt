package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.codecs.*
import com.tartner.vertx.cqrs.*
import java.util.*

/** All aggregate commands should go to this address. */
// TODO: this s/b part of the address strategy (or a "cousin")
// TODO: Should be AggregateCommandHandler as the address, and then can register a different
// handler depending on whatever
const val AggregateCommandAddress = "com.tartner.vertx.cqrs.EventSourcedAggregateCommandHandler"

@Target(AnnotationTarget.CLASS)
annotation class EventSourcedAggregate

@Target(AnnotationTarget.FUNCTION)
annotation class CreationHandler

@Target(AnnotationTarget.FUNCTION)
annotation class SnapshotHandler

/**
This is an "interface" that all event sourced aggregate verticles need to implement/handle. Since
you can't actually call a verticle directly, implementing an interface means handling all of the
commands that are defined here, and using the events when applicable.
 */
sealed class EventSourcedAggregateMessages: SerializableVertxObject

/**
This should only be fired upon a new load of the aggregate. Will return
Either<FailureReply, SuccessReply>
 */
data class ApplySnapshotAndEventsFromLoadAggregateCommand(override val aggregateId: AggregateId,
  val possibleSnapshot: AggregateSnapshot?, val events: List<AggregateEvent>):
  EventSourcedAggregateMessages(), AggregateCommand, DomainCommand by DefaultDomainCommand()

/**
If an aggregate receives this, it has become invalid and should remove all registrations because
it's going to get unloaded asap.
 */
data class InvalidateAggregateCommand(override val aggregateId: AggregateId):
  EventSourcedAggregateMessages(), AggregateCommand, DomainCommand by DefaultDomainCommand()

/**
After handling the InvalidateAggregateCommand, the aggregate should fire off this event, although
it's not saved to the event stream.
 */
data class AggregateInvalidatedEvent(val aggregateId: AggregateId,
  override val correlationId: CorrelationId): EventSourcedAggregateMessages(),
  DomainEvent

/**
Aggregates need to have commands that initialize them, typically one command. In general, the
aggregate will only want to receive the event 1 time. This will be raised when the command is
received multiple times. @See AttemptToSendCommandToUninitializedAggregateEvent
 */
data class AttemptToInitializeAlreadyInitializedAggregateEvent(override val aggregateId: UUID,
  val command: DomainCommand, override val correlationId: CorrelationId)
  : EventSourcedAggregateMessages(), ErrorEvent, HasAggregateId

/**
Occurs when you try to send a command to an aggregate that hasn't received one of it's initializing
commands.
 */
data class AttemptToSendCommandToUninitializedAggregateEvent(override val aggregateId: UUID,
  val command: DomainCommand, override val correlationId: CorrelationId):
  EventSourcedAggregateMessages(), ErrorEvent, HasAggregateId
