package com.tartner.vertx.kodein

import io.vertx.core.*
import io.vertx.core.json.*
import kotlin.reflect.*
import kotlin.reflect.full.*

interface HasDeploymentOptions {
  /** 0 means 1 deployment instance. */
  val percentOfBusThreads: Double
}

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

    val companionKclass: KClass<*>? = kclass.companionObject
    companionKclass?.let {
      if (companionKclass.isSubclassOf(HasDeploymentOptions::class)) {
        val verticleDeploymentOptions = kclass.companionObjectInstance as HasDeploymentOptions
        if (verticleDeploymentOptions.percentOfBusThreads > 0) {
          deploymentOptions.instances =
            (verticleDeploymentOptions.percentOfBusThreads * numberOfEventBusThreads).toInt()
        }
      }
    }

    val deploymentFuture = Future.future<String>()
    vertx.deployVerticle("kotlin-kodein:$className", deploymentOptions,
      deploymentFuture.completer())
    return deploymentFuture
  }
}
