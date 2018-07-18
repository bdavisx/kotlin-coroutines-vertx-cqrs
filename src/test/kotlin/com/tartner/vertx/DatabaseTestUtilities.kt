package com.tartner.vertx

import com.tartner.vertx.database.*
import io.vertx.core.*
import io.vertx.core.json.*
import io.vertx.ext.sql.*
import io.vertx.kotlin.coroutines.*

class DatabaseTestUtilities(private val queryFactory: QueryModelClientFactory) {
  suspend fun runUpdateSql(sql: String, parameters: JsonArray, vertx: Vertx,
    configuration: JsonObject): Int {

    val connection = awaitResult<SQLConnection> { handler ->
      val jdbcClient = queryFactory.create(vertx, configuration)
      jdbcClient.getConnection(handler)
    }
    val updateResult = awaitResult<UpdateResult> {connection.updateWithParams(sql, parameters, it)}
    return updateResult.updated
  }
}
