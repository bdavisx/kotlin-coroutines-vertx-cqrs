package com.tartner.vertx.kodein

import com.tartner.vertx.*
import io.vertx.core.*
import io.vertx.core.json.*
import io.vertx.core.logging.*
import org.kodein.di.*
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*

/** Used to calculate the # of verticles to deploy; default is 1 if this annotation isn't used. */
@Target(AnnotationTarget.CLASS)
annotation class PercentOfMaximumVerticleInstancesToDeploy(val percent: Short)

data class VerticleDeployment<T: Verticle>(val instance: T, val deploymentId: String)

/**
 * By default, will deploy 1 Verticle instance, but if the verticle has the
 * PercentOfMaximumVerticleInstancesToDeploy annotation, it will calculate the number of instances
 * using that value.
 */
class VerticleDeployer(
  public val maximumVerticleInstancesToDeploy: Int,
  private val kodein: Kodein
) {
  private val log = LoggerFactory.getLogger(VerticleDeployer::class.java)

  private val defaultConfig = JsonObject()

  suspend fun deployVerticles(vertx: Vertx, kclasses: List<KClass<out Verticle>>)
    : List<Future<VerticleDeployment<Verticle>>> =
      kclasses.flatMap { deployVerticle<Verticle>(vertx, it) }

  suspend fun <T: Verticle> deployVerticle(vertx: Vertx, kclass: KClass<out T>)
    : List<Future<VerticleDeployment<T>>> = deployVerticle(vertx, kclass, defaultConfig)

  suspend fun <T: Verticle> deployVerticle(vertx: Vertx, kclass: KClass<out T>, config: JsonObject)
    : List<Future<VerticleDeployment<T>>> {

    val deploymentOptions = DeploymentOptions().setWorker(false).setConfig(config)

    log.debugIf { "Attempting to create the verticle class: ${kclass.qualifiedName}" }

    val percentage = kclass.findAnnotation<PercentOfMaximumVerticleInstancesToDeploy>()
    val numberOfInstances = percentage?.let {
      (percentage.percent / 100.0 * maximumVerticleInstancesToDeploy).toInt() }
      ?: 1

    log.debugIf { "Settings number of instances to ${numberOfInstances} for ${kclass.qualifiedName}"}

    val verticleType = TT(kclass)

    val uuidFactory = kodein.direct.FactoryOrNull(TT(UUID::class.java), verticleType)
    val verticleFactory: () -> Verticle =
      if (uuidFactory != null) {
        log.debugIf { "Using a UUID factory to build the verticle" }
        // if there's a UUID factory, we need to supply the UUID. This is generally done when deploying
        // multiple instances of the same verticle
        UUIDVerticleFactory(uuidFactory, UUID.randomUUID()).factory
      } else {
        log.debugIf { "Using a 'direct' factory to build the verticle" }
        kodein.direct.AllProviders(verticleType).first()
      }

    val deploymentFutures = (1..numberOfInstances).map {
      val deploymentFuture = Future.future<VerticleDeployment<T>>()
      val verticle = verticleFactory()
      vertx.deployVerticle(verticle, deploymentOptions) { result ->
        if (result.succeeded()) {
          deploymentFuture.complete(VerticleDeployment(verticle as T, result.result()))
        } else {
          deploymentFuture.fail(result.cause())
        }
      }
      deploymentFuture
    }.toList()
    return deploymentFutures
  }

  private class UUIDVerticleFactory(
    val verticleFactory: ((UUID) -> Verticle), uuid: UUID) {
    val factory: () -> Verticle = { verticleFactory.invoke(uuid) }
  }
}
