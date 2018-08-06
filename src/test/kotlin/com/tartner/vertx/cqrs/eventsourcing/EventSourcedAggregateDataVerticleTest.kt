package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.tartner.utilities.*
import com.tartner.vertx.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.kodein.*
import io.kotlintest.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.core.json.*
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.*
import io.vertx.kotlin.core.json.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import org.junit.*
import org.junit.runner.*
import org.kodein.di.*
import org.kodein.di.generic.*
import java.util.*
import kotlin.system.*

@RunWith(VertxUnitRunner::class)
class EventSourcedAggregateDataVerticleTest: AbstractVertxTest() {
  @Test(timeout = 2500)
  fun snapshotInsertAndQuery(context: TestContext) {
    val async = context.async()

    vertx.runOnContext { launch(vertx.dispatcher()) {
      try {
        val injector = setupVertxKodein(listOf(), vertx, context).direct
        val configuration: JsonObject = buildConfiguration()

        val deployer: VerticleDeployer = injector.instance()
        val verticle = injector.i<EventSourcedAggregateDataVerticle>()
        CompositeFuture.all(deployer.deployVerticles(vertx, listOf(verticle), configuration)).await()

        val commandSender: CommandSender = injector.instance()
        val commandRegistrar: CommandRegistrar = injector.instance()

        val runtimeInMilliseconds = measureTimeMillis {
          val aggregateId = UUID.randomUUID()
          val snapshot = TestSnapshot(aggregateId, 1, "This is test data")

          val addCommand = StoreAggregateSnapshotCommand(snapshot)
          val addResult = awaitResult<Message<Either<*, *>>> {
            commandSender.send(vertx.eventBus(), addCommand, it)
          }.body()
          context.assertTrue(addResult is Either.Right)

          val loadCommand = LoadLatestAggregateSnapshotCommand(aggregateId)
          val loadResult = awaitMessageResult<Either<FailureReply, AggregateSnapshot?>> {
            commandSender.send(vertx.eventBus(), loadCommand, it)
          }

          when (loadResult) {
            is Either.Left -> context.fail()
            is Either.Right -> {
              val possibleSnapshot = loadResult.b
              if (possibleSnapshot == null) { context.fail("No snapshot was returned") }
              else {
                val loadedSnapshot: AggregateSnapshot = possibleSnapshot
                context.assertEquals(snapshot, loadedSnapshot)
              }
            }
          }

          val databaseUtils: DatabaseTestUtilities = injector.instance()
          databaseUtils.runUpdateSql(
            "delete from event_sourcing.snapshots where aggregate_id = ? and version_number = ?",
            JsonArray(aggregateId, 1), vertx, configuration)
        }

        println("Total runtime without initialization $runtimeInMilliseconds")
        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }

  @Test(timeout = 2500)
  fun eventsInsertAndQuery(context: TestContext) {
    val async = context.async()

    vertx.runOnContext { launch(vertx.dispatcher()) {
      try {
        val injector = setupVertxKodein(listOf(), vertx, context).direct
        val configuration: JsonObject = buildConfiguration()

        val deployer: VerticleDeployer = injector.instance()
        val verticle = injector.i<EventSourcedAggregateDataVerticle>()
        CompositeFuture.all(deployer.deployVerticles(vertx, listOf(verticle), configuration)).await()

        val commandSender: CommandSender = injector.instance()
        val commandRegistrar: CommandRegistrar = injector.instance()

        val runtimeInMilliseconds = measureTimeMillis {
          val aggregateId = UUID.randomUUID()

          val events = mutableListOf<AggregateEvent>()
          var aggregateVersion: Long = 0
          for (i in 1..1000) {
            events.add(EventSourcedTestAggregateCreated(aggregateId, aggregateVersion++, "Name"))
            events.add(EventSourcedTestAggregateNameChanged(aggregateId, aggregateVersion++, "New Name"))
          }

          val addCommand = StoreAggregateEventsCommand(aggregateId, events)
          val addResult = awaitResult<Message<Either<*, *>>> {
            commandSender.send(vertx.eventBus(), addCommand, it)
          }.body()
          context.assertTrue(addResult is Either.Right)

          val loadCommand =
            LoadAggregateEventsCommand(aggregateId, EventSourcingDelegate.initialVersion)
          val loadResult = awaitMessageResult<Either<FailureReply, List<AggregateEvent>>> {
            commandSender.send(vertx.eventBus(), loadCommand, it)
          }

          when (loadResult) {
            is Either.Left -> context.fail(loadResult.a.toString())
            is Either.Right -> {
              val loadedEvents: List<AggregateEvent> = loadResult.b
              if (loadedEvents.isEmpty()) {
                context.fail("No events were returned")
              }

              loadedEvents shouldEqual events
            }
          }
        }

        println("Total events runtime without initialization $runtimeInMilliseconds")
        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }

  suspend fun buildConfiguration(): JsonObject {
    val retriever = TestConfigurationDefaults.buildDefaultRetriever(vertx)
    val configuration: JsonObject = awaitResult { h -> retriever.getConfig(h) }
    return configuration
  }
}

data class TestSnapshot(override val aggregateId: UUID, override val aggregateVersion: Long,
  val testData: String): AggregateSnapshot
