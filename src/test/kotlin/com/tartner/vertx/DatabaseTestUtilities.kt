package com.tartner.vertx

import com.tartner.vertx.database.QueryModelClientFactory
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.coroutines.awaitResult

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
