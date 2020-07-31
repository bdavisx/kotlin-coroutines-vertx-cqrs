package com.tartner.vertx

import io.kotest.matchers.shouldBe
import io.vertx.core.CompositeFuture
import io.vertx.core.Context
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Random
import java.util.UUID

/** Dummy data class w/ a var for the verticle to manipulate. Wouldn't normally do this, but it's a test. */
data class ManipulateMe(var value: Int = 0)

class TestDirectCallVerticle(id: String): DirectCallVerticle(id) {
  private val log = LoggerFactory.getLogger(TestDirectCallVerticle::class.java)
  val random = Random()

  // The code in the `act` block is run on the event loop, the current thread will await on it to
  // complete
  suspend fun actFunction(manipulateMe: ManipulateMe) = act {
    logContext("act", vertx.getOrCreateContext())
    delay()
    manipulateMe.value++
  }

  // The code in the `actAndReply` block is run on the event loop, the current thread will await on
  // it to complete and the value returned by the block is returned to the caller.
  suspend fun actAndReplyFunction() = actAndReply {
    logContext("aAndR", vertx.getOrCreateContext())
    delay()
    5
  }

  // The code in the `actAndReply` block is run on the event loop, the current thread will *NOT* await on
  // it to complete and return immediately
  fun fireAndForgetFunction(future: Future<Int>) = fireAndForget {
    logContext("fAndF", vertx.getOrCreateContext())
    delay()
    future.complete(10)
  }

  private fun logContext(prefix: String, context: Context) {
    log.debug("$prefix - Context deploymentId: ${context.deploymentID()}")
  }

  private suspend fun delay() {
    awaitEvent<Long> { vertx.setTimer((random.nextInt(50) + 1).toLong(), it) }
  }
}

@RunWith(VertxUnitRunner::class)
class DirectCallVerticleTest {
  private val log = LoggerFactory.getLogger(this.javaClass)
  var vertx: Vertx = Vertx.vertx()

  @Before
  fun beforeEach(context: TestContext) {
    log.debugIf {"Running test for ${this::class.qualifiedName}"}

    vertx = Vertx.vertx()
    vertx.exceptionHandler(context.exceptionHandler())

  }

  @After
  fun afterEach(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  @Test(timeout = 2000)
  fun callFunctionsSingleDeployment(context: io.vertx.ext.unit.TestContext) {
    val async = context.async()

    vertx.runOnContext {
      GlobalScope.launch(vertx.dispatcher()) {
        try {
          vertx.eventBus().registerCodec(PassThroughCodec<CodeMessage<*>>(CodeMessage::class.qualifiedName!!))

          val deploymentOptions = DeploymentOptions()

          val id = UUID.randomUUID().toString()
          val verticle = TestDirectCallVerticle(id)
          awaitResult<String> { vertx.deployVerticle(verticle, deploymentOptions, it) }

          val manipulateMe = ManipulateMe(1)
          verticle.actFunction(manipulateMe)
          manipulateMe.value shouldBe 2

          val reply: Int = verticle.actAndReplyFunction()
          reply shouldBe 5

          val completableDeferred = Future.future<Int>()
          verticle.fireAndForgetFunction(completableDeferred)
          completableDeferred.await()

          async.complete()
        } catch(ex: Throwable) {
          context.fail(ex)
        }
      }
    }
  }

  @Test(timeout = 2000)
  fun callFunctionsMultipleDeployments(context: io.vertx.ext.unit.TestContext) {
    val async = context.async()

    vertx.runOnContext {
      GlobalScope.launch(vertx.dispatcher()) {
        try {
          vertx.eventBus().registerCodec(PassThroughCodec<CodeMessage<*>>(CodeMessage::class.qualifiedName!!))

          val deploymentOptions = DeploymentOptions()

          val id = UUID.randomUUID().toString()
          val verticlesRange = 0..20
          val verticlesAndFutures = verticlesRange.map { Pair(TestDirectCallVerticle(id), Future.future<String>()) }
          val verticles = verticlesAndFutures.map {it.first}
          verticlesAndFutures.forEach { vertx.deployVerticle(it.first, deploymentOptions, it.second.completer()) }
          CompositeFuture.all(verticlesAndFutures.map {it.second})

          val firstVerticle: TestDirectCallVerticle = verticles.first()

          verticlesRange.forEach {
            val manipulateMe = ManipulateMe(1)
            firstVerticle.actFunction(manipulateMe)
            manipulateMe.value shouldBe 2
          }

          verticlesRange.forEach {
            val reply: Int = firstVerticle.actAndReplyFunction()
            reply shouldBe 5
          }

          val returnFutures = verticles.map {
            val completableDeferred = Future.future<Int>()
            firstVerticle.fireAndForgetFunction(completableDeferred)
            completableDeferred
          }
          CompositeFuture.all(returnFutures).await()

          verticlesRange.forEach {
            val manipulateMe = ManipulateMe(1)
            firstVerticle.actFunction(manipulateMe)
            manipulateMe.value shouldBe 2
          }

          async.complete()
        } catch(ex: Throwable) {
          context.fail(ex)
        }
      }
    }
  }
}
