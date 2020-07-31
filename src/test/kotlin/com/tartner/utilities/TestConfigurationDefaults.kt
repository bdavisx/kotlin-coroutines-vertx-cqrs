package com.tartner.utilities

import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.config.ConfigRetrieverOptions
import io.vertx.kotlin.config.ConfigStoreOptions
import io.vertx.kotlin.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitResult

// TODO: move this to tests, then use the -D below to run the app
object TestConfigurationDefaults {
  fun buildDefaultRetriever(vertx: Vertx): ConfigRetriever {
    // -Dvertx-config-path=/home/bill/src/checklists-dev-configuration.json
    val storeEnvironmentOptions = ConfigStoreOptions(type = "file",
      config = JsonObject("path" to "/src/checklists-dev-configuration.json"))
    val configRetrieverOptions = ConfigRetrieverOptions(stores = listOf(storeEnvironmentOptions))
    return ConfigRetriever.create(vertx, configRetrieverOptions)!!
  }

  suspend fun buildConfiguration(vertx: Vertx): JsonObject {
    val retriever = buildDefaultRetriever(vertx)
    return awaitResult { h -> retriever.getConfig(h) }
  }
}
