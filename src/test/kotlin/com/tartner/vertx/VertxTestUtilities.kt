package com.tartner.vertx

import arrow.core.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.database.*
import com.tartner.vertx.kodein.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.ext.unit.*
import org.kodein.di.*
import org.kodein.di.generic.*
import java.time.*

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

  val verticleFactory = KodeinVerticleFactory(injector)
  vertx.registerVerticleFactory(verticleFactory)

  // TODO: these need to be in a startup class (not in test)
  vertx.eventBus().registerCodec(EventBusSerializationCodec(injector.i()))

  vertx.eventBus().registerCodec(createPassThroughCodec<CodeMessage<*>>())
  vertx.eventBus().registerDefaultCodec(Either.Left::class.java, createPassThroughCodec<Either.Left<*,*>>())
  vertx.eventBus().registerDefaultCodec(Either.Right::class.java, createPassThroughCodec<Either.Right<*,*>>())
  vertx.eventBus().registerDefaultCodec(Some::class.java, createPassThroughCodec<Some<*>>())
  vertx.eventBus().registerDefaultCodec(None::class.java, createPassThroughCodec<None>())

  return injector
}

inline fun <reified T> createPassThroughCodec() = PassThroughCodec<T>(T::class.qualifiedName!!)

val testModule = Kodein.Module {
  bind<DatabaseTestUtilities>() with singleton { DatabaseTestUtilities(i()) }

}
