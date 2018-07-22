package com.tartner.vertx.kodein

import io.vertx.core.*
import io.vertx.core.json.*
import kotlin.reflect.*
import kotlin.reflect.full.*

class VerticleDeployer {
  // TODO: this either needs to be "detected" or configurable somehow
  private val numberOfEventBusThreads = 12
  private val defaultConfig = JsonObject()

  fun deployVerticles(vertx: Vertx, kclasses: List<KClass<out Verticle>>): Future<CompositeFuture> =
    CompositeFuture.all(kclasses.map { deployVerticle(vertx, it) })

  fun deployVerticle(vertx: Vertx, kclass: KClass<out Verticle>): Future<String> =
    deployVerticle(vertx, kclass, defaultConfig)

  fun deployVerticle(vertx: Vertx, kclass: KClass<out Verticle>, config: JsonObject): Future<String> {
    val className = kclass.java.canonicalName

    val deploymentOptions = DeploymentOptions().setWorker(false).setConfig(config)

    val percentage = kclass.findAnnotation<PercentOfEventBusThreadsVerticle>()

    if (percentage != null) {
      deploymentOptions.instances =
        (percentage.percent / 100.0 * numberOfEventBusThreads).toInt()
    }

    val deploymentFuture = Future.future<String>()
    vertx.deployVerticle("kotlin-kodein:$className", deploymentOptions,
      deploymentFuture.completer())
    return deploymentFuture
  }
}
