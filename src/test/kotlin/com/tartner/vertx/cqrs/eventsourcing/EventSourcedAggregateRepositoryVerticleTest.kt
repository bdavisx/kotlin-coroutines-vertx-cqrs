package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.utilities.*
import com.tartner.vertx.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.kodein.*
import io.kotlintest.*
import io.vertx.core.*
import io.vertx.core.json.*
import io.vertx.core.logging.*
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import org.junit.*
import org.junit.runner.*
import org.kodein.di.*
import org.kodein.di.generic.*
import java.util.*

data class TestCreateEvent(override val aggregateId: UUID, override val aggregateVersion: Long,
  override val correlationId: CorrelationId): AggregateEvent
data class TestCreateSnapshot(override val aggregateId: UUID, override val aggregateVersion: Long)
  : AggregateSnapshot

class DummyVerticle: CoroutineVerticle()

@RunWith(VertxUnitRunner::class)
class EventSourcedAggregateRepositoryVerticleTest: AbstractVertxTest() {
  private val log = LoggerFactory.getLogger(EventSourcedAggregateRepositoryVerticle::class.java)

  @Test(timeout = 2500)
  fun instantiationRegistration(context: TestContext) {
    val async = context.async()
    vertx.exceptionHandler(context.exceptionHandler())

    vertx.runOnContext { launch(vertx.dispatcher()) {
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
