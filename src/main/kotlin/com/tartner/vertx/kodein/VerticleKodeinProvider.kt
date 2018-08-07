package com.tartner.vertx.kodein

import com.tartner.vertx.*
import io.vertx.core.*
import io.vertx.core.logging.*
import org.kodein.di.*
import java.util.concurrent.*
import kotlin.reflect.*
import kotlin.reflect.full.*

inline fun DKodeinAware.v() = dkodein.Instance(TT(VerticleKodeinProvider::class))

/**
 * We need to create a certain # of verticles that are deployed (or will be deployed),
 * plus we need 1 of the verticles to get injected into the DI system
 */
class VerticleKodeinProvider(public val maximumVerticleInstancesToDeploy: Int) {
  private val log = LoggerFactory.getLogger(VerticleKodeinProvider::class.java)
  private val verticleClassToVerticles = ConcurrentHashMap<KClass<out Verticle>, List<Verticle>>()
  private val verticlesInOrder = ConcurrentLinkedQueue<Verticle>()

  private fun containsVerticleClass(verticleClass: KClass<out Verticle>) =
    verticleClassToVerticles.containsKey(verticleClass)

  private fun verticle(verticleClass: KClass<out Verticle>) =
    verticleClassToVerticles[verticleClass]!!.first()

  fun verticlesToDeploy() = verticlesInOrder

  // TODO: should this by "synchronized" or is that handled by Kodein?
  fun <T: Verticle> create(verticleClass: KClass<T>, factory: () -> T): T {
    @Suppress("UNCHECKED_CAST")
    if (containsVerticleClass(verticleClass)) { return verticle(verticleClass) as T }

    log.debugIf { "Attempting to create the verticle class: ${verticleClass.qualifiedName}" }

    val numberOfInstances: Int = determineNumberOfVerticleInstances(verticleClass)

    val verticles = (1..numberOfInstances).map { factory() }
    // TODO: we must go thru and deploy these verticles once we're done
    verticleClassToVerticles[verticleClass] = verticles
    verticlesInOrder.addAll(verticles)
    return verticles.first()
  }

  fun <T: Verticle> determineNumberOfVerticleInstances(verticleClass: KClass<T>): Int {
    val percentageAnnotation = verticleClass.findAnnotation<PercentOfMaximumVerticleInstancesToDeploy>()
    val numberOfInstances: Int = if (percentageAnnotation != null) {
      (percentageAnnotation.percent / 100.0 * maximumVerticleInstancesToDeploy).toInt()
    } else {
      val countAnnotation = verticleClass.findAnnotation<SpecificNumberOfVerticleInstancesToDeploy>()
      if (countAnnotation != null) {
        countAnnotation.count
      } else {
        1
      }
    }

    log.debugIf { "Settings number of instances to ${numberOfInstances} for ${verticleClass.qualifiedName}" }
    return numberOfInstances
  }
}

/*
    val verticleType = TT(kclass)

    val uuidFactory = kodein.direct.FactoryOrNull(TT(UUID::class.java), verticleType)
    val verticleFactory: () -> Verticle =
      if (uuidFactory != null) {
        log.debugIf { "Using a UUID factory (${uuidFactory.toString()} to build the verticle" }
        // if there's a UUID factory, we need to supply the UUID. This is generally done when deploying
        // multiple instances of the same verticle
        UUIDVerticleFactory(uuidFactory, UUID.randomUUID()).factory
      } else {
        log.debugIf { "Using a 'direct/no arg' factory to build the verticle" }
        kodein.direct.AllProviders(verticleType).first()
      }

  private class UUIDVerticleFactory(
    val verticleFactory: ((UUID) -> Verticle), uuid: UUID) {
    val factory: () -> Verticle = { verticleFactory.invoke(uuid) }
  }
 */
