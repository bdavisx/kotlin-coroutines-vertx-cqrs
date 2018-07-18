package com.tartner.vertx

import io.vertx.core.*
import io.vertx.kotlin.coroutines.*
import org.kodein.di.*
import org.kodein.di.bindings.*
import org.kodein.di.generic.*
import java.lang.reflect.*
import java.util.concurrent.*
import kotlin.reflect.*

/**
 * Creates specific DirectCallVerticle subclasses
 *
 * We need to create a certain # of verticles that are deployed (or will be deployed),
 * plus we need 1 of the verticles to get injected into the DI system
 *
 * Have a class that has both the array/list of verticles and a "factory" method for Kodein to
 * use to  get an instance. Then in the module, create that and use it for both. May need to use
 * the qualified name as a qualifier for the array part
 */
class DirectCallVerticleFactory() {
  private val verticleClassToVerticle =
    ConcurrentHashMap<KClass<out DirectCallVerticle>, DirectCallVerticle>()

  internal fun containsVerticleClass(verticleClass: KClass<out DirectCallVerticle>) =
    verticleClassToVerticle.containsKey(verticleClass)

  internal fun verticle(verticleClass: KClass<out DirectCallVerticle>) =
    verticleClassToVerticle[verticleClass]!!

  /**
   * Create and register instances of a DirectCallVerticle. One issue right now is that it can only
   * handle 1 call / verticleClass - although I'm not sure that's really a problem.
   */
  suspend fun <T: DirectCallVerticle> createAndDeployVerticles(
    vertx: Vertx, kodein: Kodein, verticleClass: KClass<T>, sharedLocalId: String) {

    // TODO: calculate the # of verticles to create based on the annotation on the class and
    // # of event loops
    val numberOfInstances = 4

    val verticlesAndDeploymentIds = (0..numberOfInstances).map {
      val verticleInstance = kodein.direct.AllProviders(TT(verticleClass)).first().invoke()
      val deploymentId = awaitResult<String> { vertx.deployVerticle(verticleInstance, it) }
      Pair(verticleInstance, deploymentId)
    }

    verticleClassToVerticle.put(verticleClass, verticlesAndDeploymentIds.first().first)
  }
}

private val factory = DirectCallVerticleFactory()

val directCallKodein = Kodein {

  bind<DirectCallVerticleFactory>() with singleton { factory }

  externalSource = ExternalSource { key ->
    val jvmType: Type = key.type.jvmType
    when (jvmType) {
      DirectCallVerticleFactory::class.java -> externalFactory { factory }
      else -> {
        if (jvmType is java.lang.Class<*> &&
          DirectCallVerticle::class.java.isAssignableFrom(jvmType) &&
          factory.containsVerticleClass(jvmType as KClass<out DirectCallVerticle>)) {
          externalFactory { factory.verticle(jvmType) }
        } else {
          null
        }
      }
    }
  }
}
