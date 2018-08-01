package com.tartner.vertx.kodein

import io.vertx.core.*
import io.vertx.core.json.*
import io.vertx.core.logging.*
import org.kodein.di.*

/** Used to calculate the # of verticles to deploy; default is 1 if this annotation isn't used. */
@Target(AnnotationTarget.CLASS)
annotation class PercentOfMaximumVerticleInstancesToDeploy(val percent: Short)

data class VerticleDeployment(val instance: Verticle, val deploymentId: String)

/**
 * By default, will deploy 1 Verticle instance, but if the verticle has the
 * PercentOfMaximumVerticleInstancesToDeploy annotation, it will calculate the number of instances
 * using that value.
 */
class VerticleDeployer(private val kodein: Kodein) {
  private val log: Logger = LoggerFactory.getLogger(VerticleDeployer::class.java)

  private val defaultConfig = JsonObject()

  fun deployVerticles(vertx: Vertx, verticles: List<out Verticle>)
    : List<Future<VerticleDeployment>> = deployVerticles(vertx, verticles, defaultConfig)

  fun deployVerticles(vertx: Vertx, verticles: List<out Verticle>, config: JsonObject)
    : List<Future<VerticleDeployment>> {

    val deploymentOptions = DeploymentOptions().setWorker(false).setConfig(config)

    return verticles.map { verticle ->
      val deploymentFuture = Future.future<VerticleDeployment>()
      vertx.deployVerticle(verticle, deploymentOptions) { result ->
        if (result.succeeded()) {
          deploymentFuture.complete(VerticleDeployment(verticle, result.result()))
        } else {
          deploymentFuture.fail(result.cause())
        }
      }
      deploymentFuture
    }
  }
}
