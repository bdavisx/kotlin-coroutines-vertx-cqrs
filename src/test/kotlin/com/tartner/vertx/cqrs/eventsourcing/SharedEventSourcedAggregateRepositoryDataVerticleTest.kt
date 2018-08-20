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

@RunWith(VertxUnitRunner::class)
class SharedEventSourcedAggregateRepositoryDataVerticleTest: AbstractVertxTest() {
  private val log = LoggerFactory.getLogger(EventSourcedAggregateRepositoryVerticle::class.java)

  @Test(timeout = 2500)
  fun instantiationRegistration(context: TestContext) {
    val async = context.async()
    vertx.exceptionHandler(context.exceptionHandler())

    vertx.runOnContext { launch(vertx.dispatcher()) {
      try {
        val kodein = setupVertxKodein(vertx, context, listOf())
        val configuration: JsonObject = TestConfigurationDefaults.buildConfiguration(vertx)

        val correlationId = newId()

        val deployer: VerticleDeployer = kodein.i()
        val dKodein = kodein.direct
        CompositeFuture.all(
          deployer.deployVerticles(vertx,
            listOf(dKodein.i<SharedEventSourcedAggregateRepositoryDataVerticle>()), configuration))
          .await()

        val aggregateIds = mutableListOf<AggregateId>()
        val factory: AggregateVerticleFactory = { id: AggregateId ->
          aggregateIds.add(id)
          DummyVerticle()
        }

        val command = RegisterInstantiationClassesForAggregateLocalCommand(factory,
          listOf(TestCreateEvent::class), listOf(TestCreateSnapshot::class), correlationId)

        val commandSender: CommandSender = kodein.i()
        commandSender.send(eventBus, command)

        val factoryReply = awaitMessageResult<FindAggregateVerticleFactoryReply> {
          commandSender.send(eventBus,
            FindAggregateVerticleFactoryQuery(TestCreateEvent::class, correlationId), it)
        }

        factoryReply.factory shouldBe factory

        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }
    }
  }
}
