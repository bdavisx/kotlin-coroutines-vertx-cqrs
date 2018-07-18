package com.tartner.vertx.kodein

import com.tartner.vertx.*
import io.vertx.core.*
import io.vertx.core.logging.*
import io.vertx.core.spi.*
import org.kodein.di.*

/**
  Represents verticle factory which uses Kodein for verticle creation.

  1) This factory should be registered in Vertx.
  2) Verticle should be deployed with the factory prefix `"kotlin-kodein:"`
 */
class KodeinVerticleFactory(val kodein: Kodein): VerticleFactory {
  companion object {
    private val log = LoggerFactory.getLogger(KodeinVerticleFactory::class.java)

    const val prefix = "kotlin-kodein"
  }

  override fun prefix(): String = prefix

  override fun createVerticle(name: String, classLoader: ClassLoader): Verticle {
    val verticleClassName = VerticleFactory.removePrefix(name)
    val verticleClass: Class<Any> = (classLoader.loadClass(verticleClassName) as Class<Any>?)!!

    log.debugIf { "Attempting to create the verticle class: '$name': class - $verticleClass" }
    val verticleInstance = kodein.direct.AllProviders(TT(verticleClass)).first().invoke()
    return verticleInstance as Verticle
  }
}
