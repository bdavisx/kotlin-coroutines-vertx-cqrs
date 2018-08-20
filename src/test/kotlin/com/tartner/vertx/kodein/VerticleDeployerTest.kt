package com.tartner.vertx.kodein

import com.tartner.vertx.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
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
        val kodein = setupVertxKodein(vertx, context, listOf(testModule))

        val deployer: VerticleDeployer = kodein.i()

        val futures = deployer.deployVerticles(vertx, listOf(SimpleVerticle()))

        futures.count() shouldBe 1

        val deploymentFuture: Future<VerticleDeployment> = futures.first()
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
}

val testModule = Kodein.Module("VerticleDeployerTest module") {
  bind<SimpleVerticle>() with provider { SimpleVerticle() }
  bind<MultipleDeploymentVerticle>() with factory {id: UUID -> MultipleDeploymentVerticle(i())}
}

class SimpleVerticle(): CoroutineVerticle()

class IncrementCommand(
  override val correlationId: CorrelationId = newId()): DomainCommand

/*
 * In general, you *don't* want to have any local data that is variable on the multiple deployment
 * verticles, they s/b stateless service verticles. If you do need to access the instance that the
 * code is running in, `it` is passed in as the verticle in the code block - see the `increment`
 * function below. Otherwise `this` is actually captured in the lambda, so it may not be the
 * one you expect when the code runs.
 */
@PercentOfMaximumVerticleInstancesToDeploy(50)
class MultipleDeploymentVerticle(
  private val commandRegistrar: CommandRegistrar
): CoroutineVerticle() {
  private val log = LoggerFactory.getLogger(MultipleDeploymentVerticle::class.java)
  var counter: Int = 0  // DON'T usually want anything like this in a multi instance verticle

  override suspend fun start() {
    super.start()
    commandRegistrar.registerLocalCommandHandler(eventBus, IncrementCommand::class, {increment()})
  }

  fun increment() {
    log.debug("Incrementing counter")
    counter++
  }

  override fun toString(): String {
    return "MultipleDeploymentVerticle(counter=$counter)"
  }
}
