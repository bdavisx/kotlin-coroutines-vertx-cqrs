package com.tartner.vertx.commands

import com.tartner.vertx.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.kodein.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.core.logging.*
import io.vertx.ext.unit.*
import io.vertx.ext.unit.junit.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import org.junit.*
import org.junit.runner.*
import org.kodein.di.*
import org.kodein.di.generic.*

private val log = LoggerFactory.getLogger(CommandSenderTest::class.java)

@RunWith(VertxUnitRunner::class)
class CommandSenderTest: AbstractVertxTest() {
  @Test(timeout = 1500)
  fun testKodein(testContext: TestContext) {
    val kodein = setupVertxKodein(listOf(localTestModule), vertx, testContext)

    val deployer: VerticleDeployer = kodein.i()
  }

  @Test(timeout = 1500)
  fun testItShouldSendACommandCorrectly(testContext: TestContext) {
    val kodein = setupVertxKodein(listOf(localTestModule), vertx, testContext)

    val async = testContext.async()

    vertx.exceptionHandler(testContext.exceptionHandler())

    // screw this test this way, create a verticle that registers for a command and replies or
    // sends an event, much simpler
    vertx.runOnContext { launch(vertx.dispatcher()) {
      val deployer: VerticleDeployer = kodein.i()
      CompositeFuture.all(deployer.deployVerticles(vertx,
        listOf(CommandSenderTestVerticle(kodein.i(), kodein.i())))).await()

      val sender: CommandSender = kodein.i()
      val command = DummyCommand(1)
      log.debug("Sending message")
      awaitResult<Message<Any>> { sender.send(eventBus, command, it) }
      log.debug("got reply")

      async.complete()
    }}
  }
}

val localTestModule = Kodein.Module {
  bind<CommandSenderTestVerticle>() with provider { CommandSenderTestVerticle(i(), i()) }
}

data class DummyCommand(val value: Int): SerializableVertxObject

class CommandSenderTestVerticle(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender
): CoroutineVerticle() {
  override suspend fun start() {
    commandRegistrar.registerLocalCommandHandler(eventBus, DummyCommand::class, Handler {
      log.debug("Got message in TestVerticle")
      commandSender.reply(it, SuccessReply)
    })
  }
}
