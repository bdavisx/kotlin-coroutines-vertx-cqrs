package com.tartner.utilities

import io.vertx.config.*
import io.vertx.core.*
import io.vertx.core.json.*
import io.vertx.kotlin.config.*
import io.vertx.kotlin.core.json.*
import io.vertx.kotlin.coroutines.*

// TODO: move this to tests, then use the -D below to run the app
object TestConfigurationDefaults {
  fun buildDefaultRetriever(vertx: Vertx): ConfigRetriever {
    // -Dvertx-config-path=/home/bill/src/checklists-dev-configuration.json
    val storeEnvironmentOptions = ConfigStoreOptions(type = "file",
      config = JsonObject("path" to "/home/bill/src/checklists-dev-configuration.json"))
    val configRetrieverOptions = ConfigRetrieverOptions(stores = listOf(storeEnvironmentOptions))
    return ConfigRetriever.create(vertx, configRetrieverOptions)!!
  }

  suspend fun buildConfiguration(vertx: Vertx): JsonObject {
    val retriever = TestConfigurationDefaults.buildDefaultRetriever(vertx)
    return awaitResult { h -> retriever.getConfig(h) }
  }
}
