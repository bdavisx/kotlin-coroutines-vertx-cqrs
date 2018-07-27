package com.tartner.vertx.kodein

import com.tartner.utilities.*
import com.tartner.vertx.*
import io.kotlintest.*
import io.vertx.core.*
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

@RunWith(VertxUnitRunner::class)
class VerticleDeployerTest: AbstractVertxTest() {
  private val log = LoggerFactory.getLogger(VerticleDeployerTest::class.java)
  @Test(timeout = 2500)
  fun singleDeployment(context: TestContext) {
    val async = context.async()
    vertx.exceptionHandler(context.exceptionHandler())

    vertx.runOnContext { launch(vertx.dispatcher()) {
      try {
        val kodein = setupVertxKodein(listOf(testModule), vertx, context)

        val deployer: VerticleDeployer = kodein.i()

        val futures = deployer.deployVerticle(vertx, SimpleVerticle::class)

        futures.count() shouldBe 1

        val deploymentFuture: Future<VerticleDeployment<SimpleVerticle>> = futures.first()
        val deployment = deploymentFuture.await()

        deploymentFuture.succeeded() shouldBe true
        deployment.deploymentId.isBlank() shouldBe false
        log.debug(deployment)

        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }

  @Test(timeout = 2500)
  fun multipleDeployments(context: TestContext) {
    val async = context.async()
    vertx.exceptionHandler(context.exceptionHandler())

    vertx.runOnContext { launch(vertx.dispatcher()) {
      try {
        val kodein = setupVertxKodein(listOf(testModule), vertx, context)

        val deployer: VerticleDeployer = kodein.i()

        val futures = deployer.deployVerticle(vertx, MultipleDeploymentVerticle::class)

        val expectedNumberOfVerticles = 4
        futures.count() shouldBe expectedNumberOfVerticles

        val allFutures = CompositeFuture.all(futures).await()
        allFutures.succeeded() shouldBe true

        futures.map { it.result().deploymentId }.distinct().count() shouldBe expectedNumberOfVerticles
        futures.map { it.result().instance.localAddress }
          .distinct().count() shouldBe 1

        val verticle = futures.first().result().instance
        (1..expectedNumberOfVerticles).forEach { verticle.increment() }
        futures.forEach { it.result().instance.counter shouldBe 1 }

        async.complete()
      } catch(ex: Throwable) {
        context.fail(ex)
      }
    }}
  }
}

val testModule = Kodein.Module {
  bind<SimpleVerticle>() with provider { SimpleVerticle() }
  bind<MultipleDeploymentVerticle>() with factory {id: UUID -> MultipleDeploymentVerticle(id)}
}

class SimpleVerticle(): CoroutineVerticle()

/*
 * In general, you *don't* want to have any local data that is variable on the multiple deployment
 * verticles, they s/b stateless service verticles. If you do need to access the instance that the
 * code is running in, `it` is passed in as the verticle in the code block - see the `increment`
 * function below. Otherwise `this` is actually captured in the lambda, so it may not be the
 * one you expect when the code runs.
 */
@PercentOfMaximumVerticleInstancesToDeploy(50)
class MultipleDeploymentVerticle(id: UUID): DirectCallVerticle(id.toStringFast()) {
  private val log = LoggerFactory.getLogger(MultipleDeploymentVerticle::class.java)
  var counter: Int = 0  // DON'T usually want anything like this in a multi instance verticle

  suspend fun increment() = act {
    log.debug("Incrementing counter")
    (it as MultipleDeploymentVerticle).counter++
  }

  override fun toString(): String {
    return "MultipleDeploymentVerticle(localAddress=$localAddress; counter=$counter)"
  }
}
