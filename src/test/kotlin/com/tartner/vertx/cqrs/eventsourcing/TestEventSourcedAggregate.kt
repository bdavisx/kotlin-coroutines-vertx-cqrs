package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.tartner.kamedon.validation.*
import com.tartner.vertx.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.functional.*

sealed class TestEventSourcedAggregateCommands(): AggregateCommand
sealed class TestEventSourcedAggregateEvents(): AggregateEvent

data class CreateTestEventSourcedAggregateCommand(
  override val aggregateId: AggregateId, val name: String): TestEventSourcedAggregateCommands()

data class TestEventSourcedAggregateCreated(override val aggregateId: AggregateId,
  override val aggregateVersion: Long, val name: String): TestEventSourcedAggregateEvents()

data class TestEventSourcedAggregateNameChanged(override val aggregateId: AggregateId,
  override val aggregateVersion: Long, val name: String): TestEventSourcedAggregateEvents()

data class CreateTestEventSourcedAggregateValidationFailed(val validationIssues: ValidationIssues): FailureReply

typealias TestEventSourcedAggregateCreateReply =
  Either<FailureReply, List<TestEventSourcedAggregateEvents>>

interface TestEventSourcedAggregateQuery {
  fun aggregateIdExists(aggregateId: AggregateId): Boolean
}

@EventSourcedAggregate
class TestEventSourcedAggregate(
  private val aggregateId: AggregateId,
  private val eventSourcingDelegate: EventSourcingDelegate,
  private val testEventSourcedAggregateQuery: TestEventSourcedAggregateQuery
): DirectCallVerticle() {

  private lateinit var name: String

  suspend fun createAggregate(command: CreateTestEventSourcedAggregateCommand)
    : TestEventSourcedAggregateCreateReply = actAndReply {

    val events = validateCreateCommand(command).flatMap { validatedCommand ->
        val aggregateVersion = eventSourcingDelegate.firstVersion(validatedCommand)
        val versionFold = aggregateVersion.fold(
          {left -> CreateAggregateVersionFailed(left).createLeft()},
          {version -> listOf(TestEventSourcedAggregateCreated(aggregateId, version, validatedCommand.name))
            .createRight()})
        versionFold
      }

    events.mapS {
      applyEvents(it)
      eventSourcingDelegate.storeAndPublishEvents(it, eventBus)
    }

    events
  }

  private fun applyEvents(events: List<TestEventSourcedAggregateEvents>) {
    events.forEach { event ->
      when (event) {
        is TestEventSourcedAggregateCreated -> { name = event.name }
        is TestEventSourcedAggregateNameChanged -> { name = event.name }
      }
    }
  }

  private fun validateCreateCommand(command: CreateTestEventSourcedAggregateCommand)
    : Either<CreateTestEventSourcedAggregateValidationFailed, CreateTestEventSourcedAggregateCommand> {

    /* Validation: aggregateId must not exist, name must not be blank. */
    val validation = Validation<CreateTestEventSourcedAggregateCommand> {
      "aggregateId" {
        mustBe {
          !testEventSourcedAggregateQuery.aggregateIdExists(command.aggregateId)
        } ifNot "aggregateId must not already exist"

        "name" {
          mustBe { !command.name.isBlank() } ifNot "name: must not be blank"
        }
      }
    }

    val possibleValidationIssues: ValidationIssues? = validation.validate(command)

    possibleValidationIssues?.let {
      return CreateTestEventSourcedAggregateValidationFailed(possibleValidationIssues).createLeft()
    }

    // TODO: we should sanitize the inputs???
    return command.createRight()
  }

}
