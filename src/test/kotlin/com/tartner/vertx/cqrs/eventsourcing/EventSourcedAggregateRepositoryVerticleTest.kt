package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.tartner.utilities.*
import com.tartner.vertx.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.functional.*
import com.tartner.vertx.kodein.*
import io.kotlintest.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.core.json.*
import io.vertx.core.logging.*
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import org.junit.*
import org.junit.runner.*
import org.kodein.di.*
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
        val kodein = setupVertxKodein(vertx, context, listOf())
        val configuration: JsonObject = TestConfigurationDefaults.buildConfiguration(vertx)

        val commandRegistrar: CommandRegistrar = kodein.i()
        val commandSender: CommandSender = kodein.i()

        val correlationId = newId()
        val aggregateId = newId()

        commandRegistrar.registerLocalCommandHandler(eventBus, IsAggregateDeployedQuery::class,
          {it: Message<IsAggregateDeployedQuery> ->
            commandSender.reply(it, IsAggregateDeployedQueryReply(false))})

        commandRegistrar.registerLocalCommandHandler(eventBus, LoadLatestAggregateSnapshotCommand::class,
          {it: Message<LoadLatestAggregateSnapshotCommand> ->
            commandSender.reply(it, Either.Right(null))})

        commandRegistrar.registerLocalCommandHandler(eventBus, LoadAggregateEventsCommand::class,
          {it: Message<LoadAggregateEventsCommand> ->
            commandSender.reply(it, mutableListOf(
              TestCreateEvent(aggregateId, EventSourcingDelegate.initialVersion, correlationId))
              .createRight())})

        commandRegistrar.registerLocalCommandHandler(eventBus, FindAggregateVerticleFactoryQuery::class,
          {it: Message<FindAggregateVerticleFactoryQuery> ->
            commandSender.reply(it, Either.Right(null))})

        val deployer: VerticleDeployer = kodein.i()
        val dKodein = kodein.direct
        CompositeFuture.all(
          deployer.deployVerticles(vertx,
            listOf(dKodein.i<EventSourcedAggregateRepositoryVerticle>()), configuration))
          .await()

        val reply = awaitMessageEitherResult<SuccessReply> { commandSender.send(eventBus,
          LoadEventSourcedAggregateCommand(aggregateId, aggregateId.toString(), correlationId), it) }

        reply shouldNotBe null

        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }
}
