package com.tartner.vertx

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import com.tartner.vertx.codecs.EventBusJacksonJsonCodec
import com.tartner.vertx.database.databaseFactoryModule
import com.tartner.vertx.kodein.i
import com.tartner.vertx.kodein.vertxKodeinModule
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.ext.unit.TestContext
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import java.time.Duration

val Int.seconds get() = Duration.ofSeconds(this.toLong())
val Int.milliSeconds get() = Duration.ofMillis(this.toLong())

val FastActorResponseTime: Duration = 10.milliSeconds

val AbstractVertxTest.eventBus: EventBus
  get() = vertx.eventBus()

fun setupVertxKodein(modules: Iterable<Kodein.Module>, vertx: Vertx, testContext: TestContext)
  : Kodein {

  vertx.exceptionHandler(testContext.exceptionHandler())

  val modulesWithVertx =
    mutableListOf(vertxKodeinModule(vertx), libraryModule, databaseFactoryModule, testModule)

  modulesWithVertx.addAll(modules)
  val injector = Kodein { modulesWithVertx.forEach { import(it) } }

  // TODO: these need to be in a startup class (not in test)
  vertx.eventBus().registerCodec(EventBusJacksonJsonCodec(injector.i()))

  vertx.eventBus().registerCodec(createPassThroughCodec<CodeMessage<*>>())
  vertx.eventBus().registerDefaultCodec(Either.Left::class.java, createPassThroughCodec<Either.Left<*>>())
  vertx.eventBus().registerDefaultCodec(Either.Right::class.java, createPassThroughCodec<Either.Right<*>>())
  vertx.eventBus().registerDefaultCodec(Some::class.java, createPassThroughCodec<Some<*>>())
  vertx.eventBus().registerDefaultCodec(None::class.java, createPassThroughCodec<None>())

  return injector
}

inline fun <reified T> createPassThroughCodec() = PassThroughCodec<T>(T::class.qualifiedName!!)

val testModule = Kodein.Module("testModule-VertxTestUtilities.kt") {
  bind<DatabaseTestUtilities>() with singleton { DatabaseTestUtilities(i()) }
}
