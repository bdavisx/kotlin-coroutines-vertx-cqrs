package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.utilities.TestConfigurationDefaults
import com.tartner.vertx.AbstractVertxTest
import com.tartner.vertx.awaitMessageEitherResult
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.cqrs.AggregateEvent
import com.tartner.vertx.cqrs.AggregateId
import com.tartner.vertx.cqrs.AggregateSnapshot
import com.tartner.vertx.eventBus
import com.tartner.vertx.kodein.VerticleDeployer
import com.tartner.vertx.kodein.i
import com.tartner.vertx.setupVertxKodein
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.vertx.core.CompositeFuture
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.util.UUID

data class TestCreateEvent(override val aggregateId: UUID, override val aggregateVersion: Long): AggregateEvent
data class TestCreateSnapshot(override val aggregateId: UUID, override val aggregateVersion: Long): AggregateSnapshot

class DummyVerticle: CoroutineVerticle()

@RunWith(VertxUnitRunner::class)
class EventSourcedAggregateRepositoryVerticleTest: AbstractVertxTest() {
  private val log = LoggerFactory.getLogger(EventSourcedAggregateRepositoryVerticle::class.java)

  @Test(timeout = 2500)
  fun instantiationRegistration(context: TestContext) {
    val async = context.async()
    vertx.exceptionHandler(context.exceptionHandler())

    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
      try {
        val kodein = setupVertxKodein(listOf(), vertx, context)
        val configuration: JsonObject = TestConfigurationDefaults.buildConfiguration(vertx)

        val deployer: VerticleDeployer = kodein.i()
        CompositeFuture.all(
          deployer.deployVerticles(vertx,
            listOf(kodein.direct.instance<EventSourcedAggregateRepositoryVerticle>()), configuration))
          .await()

        val aggregateIds = mutableListOf<AggregateId>()
        val factory: AggregateVerticleFactory = { id: AggregateId ->
          aggregateIds.add(id)
          DummyVerticle()
        }

        val command = RegisterInstantiationClassesForAggregateLocalCommand(factory,
          listOf(TestCreateEvent::class), listOf(TestCreateSnapshot::class))

        val commandSender: CommandSender = kodein.i()
        awaitMessageEitherResult<Any> { commandSender.send(eventBus, command, it) }

        val snapshot = awaitMessageEitherResult<SharedEventSourcedAggregateRepositoryDataSnapshot> {
          commandSender.send(eventBus, SharedEventSourcedAggregateRepositorySnapshotQuery, it)
        }

        snapshot.instantiationClassToFactory.size shouldBe 2
        snapshot.instantiationClassToFactory.containsKey(TestCreateEvent::class) shouldBe true
        snapshot.instantiationClassToFactory[TestCreateSnapshot::class] shouldNotBe null

        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }
}
