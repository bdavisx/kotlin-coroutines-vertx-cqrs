package com.tartner.vertx

import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.functional.*
import com.tartner.vertx.kodein.*
import io.kotlintest.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.core.logging.*
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import org.junit.*
import org.junit.runner.*

private val log = LoggerFactory.getLogger(AwaitUtilitiesTest::class.java)

class ReturnRegularReply(): SerializableVertxObject
class ReturnEitherReply(): SerializableVertxObject
class RegularReply(): SerializableVertxObject

const val testVerticleAddress = "testVerticle"
const val replyVerticleAddress = "replyVerticle"

class AwaitUtilitiesTestVerticle(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender
): CoroutineVerticle() {

  override suspend fun start() {
    super.start()
    commandRegistrar.registerCommandHandlerWithLocalAddress(eventBus, testVerticleAddress,
      Handler<Message<SerializableVertxObject>> { message(it) } )
  }

  fun message(it: Message<SerializableVertxObject>) {
    val command = it.body()
    when (command) {
      is ReturnRegularReply -> commandSender.reply(it, RegularReply())
      is ReturnEitherReply -> commandSender.reply(it, RegularReply().createRight())
    }
  }

  // need to send message and get back an either reply; needs to be a different verticle, otherwise
  // you get "deadlocked"
}

class AwaitUtilitiesTestReplyVerticle(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender
): CoroutineVerticle() {
}

@RunWith(VertxUnitRunner::class)
class AwaitUtilitiesTest(): AbstractVertxTest() {
  @Test(timeout = 1000)
  fun awaitMessageResultTest(testContext: TestContext) {
    val kodein = setupVertxKodein(listOf(), vertx, testContext)

    val async = testContext.async()

    vertx.exceptionHandler(testContext.exceptionHandler())

    vertx.runOnContext { launch(vertx.dispatcher()) {

      val commandRegistrar: CommandRegistrar = kodein.i()
      val commandSender: CommandSender = kodein.i()

      val deployer: VerticleDeployer = kodein.i()
      CompositeFuture.all(
        deployer.deployVerticles(vertx,
          listOf(AwaitUtilitiesTestVerticle(commandRegistrar, commandSender)))).await()

      val response =
        awaitMessageResult<RegularReply> {
          commandSender.send(eventBus, testVerticleAddress, ReturnRegularReply(), it) }

      async.complete()
    }}
  }

  // TODO: need to figure out how to text the failure/exception part of awaitMessageResult

  @Test(timeout = 1000)
  fun awaitMessageEitherResult(testContext: TestContext) {
    val kodein = setupVertxKodein(listOf(), vertx, testContext)

    val async = testContext.async()

    vertx.exceptionHandler(testContext.exceptionHandler())

    vertx.runOnContext { launch(vertx.dispatcher()) {

      val commandRegistrar: CommandRegistrar = kodein.i()
      val commandSender: CommandSender = kodein.i()

      val deployer: VerticleDeployer = kodein.i()
      CompositeFuture.all(
        deployer.deployVerticles(vertx,
          listOf(AwaitUtilitiesTestVerticle(commandRegistrar, commandSender)))).await()

      val response =
        awaitMessageEitherResult<RegularReply> {
          commandSender.send(eventBus, testVerticleAddress, ReturnEitherReply(), it) }

      async.complete()
    }}
  }
}
