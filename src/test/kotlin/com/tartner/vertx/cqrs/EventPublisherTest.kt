package com.tartner.vertx.cqrs

import com.tartner.vertx.*
import com.tartner.vertx.kodein.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.ext.unit.*
import io.vertx.ext.unit.junit.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import org.junit.*
import org.junit.runner.*
import org.kodein.di.*
import org.kodein.di.generic.*
import kotlin.system.*

data class EventPublisherTestEvent(val id: String, val receiveFuture: Future<String>,
  override val correlationId: CorrelationId): DomainEvent

class EventPublisherTestVerticle(
  val eventRegistrar: EventRegistrar
): CoroutineVerticle() {
  override suspend fun start() {
    super.start()
    eventRegistrar.registerEventHandler(eventBus, EventPublisherTestEvent::class,
      Handler {eventHandler(it)})
  }

  private fun eventHandler(eventMessage: Message<EventPublisherTestEvent>) {
    val event = eventMessage.body()
    event.receiveFuture.complete(event.id)
  }
}

val testModule = Kodein.Module("EventPublisherTest module") {
  bind<EventPublisherTestVerticle>() with provider { EventPublisherTestVerticle(i()) }
}

@RunWith(VertxUnitRunner::class)
class EventPublisherTest(): AbstractVertxTest() {
  @Test(timeout = 1000)
  fun publish(context: TestContext) {
    val async = context.async()

    vertx.runOnContext { launch(vertx.dispatcher()) {
      try {
        val injector = setupVertxKodein(listOf(testModule), vertx, context).direct

        val deployer: VerticleDeployer = injector.instance()
        val verticle = injector.i<EventPublisherTestVerticle>()
        val eventPublisher: EventPublisher = injector.i()
        CompositeFuture.all(deployer.deployVerticles(vertx, listOf(verticle))).await()

        val id = "1"
        val runtimeInMilliseconds = measureTimeMillis {
          val eventFuture = Future.future<String>()
          val event = EventPublisherTestEvent(id, eventFuture, newId())
          eventPublisher.publish(event)
          CompositeFuture.all(listOf(eventFuture)).await()
        }

        println("Total runtime without initialization $runtimeInMilliseconds")
        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }
    }
  }

}
