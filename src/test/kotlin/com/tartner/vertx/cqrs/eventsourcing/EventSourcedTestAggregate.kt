package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.tartner.kamedon.validation.*
import com.tartner.vertx.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.functional.*

sealed class TestEventSourcedAggregateCommands(): AggregateCommand
sealed class TestEventSourcedAggregateEvents(): AggregateEvent

data class CreateEventSourcedTestAggregateCommand(
  override val aggregateId: AggregateId, val name: String)
  : TestEventSourcedAggregateCommands(), AggregateCommand by DefaultAggregateCommand(aggregateId)

data class ChangeEventSourcedTestAggregateNameCommand(
  override val aggregateId: AggregateId, val name: String)
  : TestEventSourcedAggregateCommands(), AggregateCommand by DefaultAggregateCommand(aggregateId)

data class EventSourcedTestAggregateCreated(override val aggregateId: AggregateId,
  override val aggregateVersion: Long, val name: String): TestEventSourcedAggregateEvents()

data class EventSourcedTestAggregateNameChanged(override val aggregateId: AggregateId,
  override val aggregateVersion: Long, val name: String): TestEventSourcedAggregateEvents()

data class CreateEventSourcedTestAggregateValidationFailed(val validationIssues: ValidationIssues)
  : FailureReply

interface TestEventSourcedAggregateQuery {
  suspend fun aggregateIdExists(aggregateId: AggregateId): Boolean
}

@EventSourcedAggregate
class EventSourcedTestAggregate(
  private val aggregateId: AggregateId,
  private val eventSourcingDelegate: EventSourcingDelegate,
  private val testEventSourcedAggregateQuery: TestEventSourcedAggregateQuery
): DirectCallVerticle() {

  private lateinit var name: String

  private fun applyEvents(events: List<TestEventSourcedAggregateEvents>) {
    events.forEach { event ->
      when (event) {
        is EventSourcedTestAggregateCreated -> { name = event.name }
        is EventSourcedTestAggregateNameChanged -> { name = event.name }
      }
    }
  }

  suspend fun createAggregate(command: CreateEventSourcedTestAggregateCommand)
    : Either<FailureReply, List<TestEventSourcedAggregateEvents>> = actAndReply {

    val possibleEvents = validateCreateCommand(command).flatMap { validatedCommand ->
      val aggregateVersion = eventSourcingDelegate.firstVersion(validatedCommand)
      aggregateVersion.fold(
        {left -> CreateAggregateVersionFailed(left).createLeft()},
        {version ->
          listOf(EventSourcedTestAggregateCreated(aggregateId, version, validatedCommand.name))
            .createRight()})
    }

    possibleEvents.mapS {
      applyEvents(it)
      eventSourcingDelegate.storeAndPublishEvents(it, eventBus)
    }

    possibleEvents
  }

  suspend fun updateName(command: ChangeEventSourcedTestAggregateNameCommand) {

  }

  private suspend fun validateCreateCommand(command: CreateEventSourcedTestAggregateCommand)
    : Either<CreateEventSourcedTestAggregateValidationFailed, CreateEventSourcedTestAggregateCommand> {

    // TODO: we should sanitize the inputs

    /* Validation: aggregateId must not exist, name must not be blank. */
    val validation = Validation<CreateEventSourcedTestAggregateCommand> {
      "aggregateId" {
        mustBe { testEventSourcedAggregateQuery.aggregateIdExists(command.aggregateId)
          } ifNot "aggregateId must not already exist"
        }

        "name" {
          mustBe { !command.name.isBlank() } ifNot "name: must not be blank"
        }
      }

    val possibleValidationIssues: ValidationIssues? = validation.validate(command)

    possibleValidationIssues?.let {
      return CreateEventSourcedTestAggregateValidationFailed(possibleValidationIssues).createLeft()
    }

    return command.createRight()
  }

}
