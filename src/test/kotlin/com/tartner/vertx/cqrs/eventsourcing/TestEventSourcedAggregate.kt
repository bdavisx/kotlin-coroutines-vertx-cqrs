package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.kamedon.validation.*
import com.tartner.vertx.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.functional.*

sealed class TestEventSourcedAggregateCommands(): AggregateCommand
sealed class TestEventSourcedAggregateEvents(): AggregateEvent

data class CreateTestEventSourcedAggregateCommand(
  override val aggregateId: AggregateId, val name: String): TestEventSourcedAggregateCommands()

data class TestEventSourcedAggregateCreated(override val aggregateId: AggregateId,
  override val aggregateVersion: Long, val name: String): TestEventSourcedAggregateEvents()

class FReply(): FailureReply

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

    val validation = validateCreateCommand(command)

//    var reply = if (!command.name.isEmpty()) {
//
//    }

    FReply().createLeft()
  }

  private fun validateCreateCommand(command: CreateTestEventSourcedAggregateCommand)
    : Validation<CreateTestEventSourcedAggregateCommand> {

    /* Validation: aggregateId must not exist, name must not be blank. */
    val validation = Validation<CreateTestEventSourcedAggregateCommand> {
      "aggregateId" {
        mustBe {
          !testEventSourcedAggregateQuery.aggregateIdExists(command.aggregateId)
        } ifNot "aggregateId must not already exist"

        "name"{
          mustBe { command.name.length >= 1 } ifNot "name: 1 character or more"
        }
      }
    }
    return validation
  }

}
