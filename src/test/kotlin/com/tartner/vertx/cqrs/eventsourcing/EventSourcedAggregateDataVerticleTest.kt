package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.tartner.utilities.*
import com.tartner.vertx.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.kodein.*
import io.kotlintest.*
import io.vertx.core.*
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
  fun snapshotInsertAndQuery(context: io.vertx.ext.unit.TestContext) {
    val async = context.async()

    vertx.runOnContext { launch(vertx.dispatcher()) {
      try {
        val injector = setupVertxKodein(listOf(), vertx, context)
        val configuration: JsonObject = TestConfigurationDefaults.buildConfiguration(vertx)
        val deploymentOptions = DeploymentOptions()
        deploymentOptions.config = configuration

        val deployer: VerticleDeployer = injector.i()
        val factory = injector.direct.factory<String, EventSourcedAggregateDataVerticle>()
        val verticle: EventSourcedAggregateDataVerticle = factory(UUID.randomUUID().toString())
        awaitResult<String> { vertx.deployVerticle(verticle, deploymentOptions, it) }

        val commandSender: CommandSender = injector.i()
        val commandRegistrar: CommandRegistrar = injector.i()

        val runtimeInMilliseconds = measureTimeMillis {
          val aggregateId = UUID.randomUUID()
          val snapshot = TestSnapshot(aggregateId, 1, "This is test data")


          val addResult = verticle.storeAggregateSnapshot(snapshot)
          context.assertTrue(addResult is Either.Right)

          val loadResult = verticle.loadLatestAggregateSnapshot(aggregateId)

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

          val databaseUtils: DatabaseTestUtilities = injector.i()
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

  @Test(timeout = 5000)
  fun eventsInsertAndQuery(context: TestContext) {
    val async = context.async()

    vertx.runOnContext { launch(vertx.dispatcher()) {
      try {
        val injector = setupVertxKodein(listOf(), vertx, context)
        val configuration: JsonObject = TestConfigurationDefaults.buildConfiguration(vertx)
        val deploymentOptions = DeploymentOptions()
        deploymentOptions.config = configuration

        val deployer: VerticleDeployer = injector.i()
        val factory = injector.direct.factory<String, EventSourcedAggregateDataVerticle>()
        val verticle: EventSourcedAggregateDataVerticle = factory(UUID.randomUUID().toString())
        awaitResult<String> { vertx.deployVerticle(verticle, deploymentOptions, it) }

        val commandSender: CommandSender = injector.i()
        val commandRegistrar: CommandRegistrar = injector.i()

        val runtimeInMilliseconds = measureTimeMillis {
          val aggregateId = UUID.randomUUID()

          val events = mutableListOf<AggregateEvent>()
          var aggregateVersion: Long = 0
          for (i in 1..1000) {
            events.add(TestEventSourcedAggregateCreated(aggregateId, aggregateVersion++, "Name"))
            events.add(TestEventSourcedAggregateNameChanged(aggregateId, aggregateVersion++, "New Name"))
          }

          val addResult = verticle.storeAggregateEvents(aggregateId, events)
          context.assertTrue(addResult is Either.Right)

          val loadResult =
            verticle.loadAggregateEvents(aggregateId, EventSourcingDelegate.initialVersion)

          when (loadResult) {
            is Either.Left -> context.fail(loadResult.a.toString())
            is Either.Right -> {
              val loadedEvents: List<AggregateEvent> = loadResult.b
              if (loadedEvents.isEmpty()) {
                context.fail("No events were returned")
              }

              loadedEvents shouldBe events
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
}

data class TestSnapshot(override val aggregateId: UUID, override val aggregateVersion: Long,
  val testData: String): AggregateSnapshot
