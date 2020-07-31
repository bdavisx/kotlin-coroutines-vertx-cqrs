package com.tartner.vertx.commands

import com.tartner.vertx.AbstractVertxTest
import com.tartner.vertx.codecs.SerializableVertxObject
import com.tartner.vertx.cqrs.SuccessReply
import com.tartner.vertx.eventBus
import com.tartner.vertx.kodein.VerticleDeployer
import com.tartner.vertx.kodein.i
import com.tartner.vertx.setupVertxKodein
import io.vertx.core.CompositeFuture
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.provider

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
    vertx.runOnContext { GlobalScope.launch(vertx.dispatcher()) {
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
