package com.tartner.vertx

import arrow.core.Either
import com.tartner.vertx.codecs.SerializableVertxObject
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.cqrs.FailureReply
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.awaitResult

suspend fun <T> awaitMessageResult(block: (h: Handler<AsyncResult<Message<T>>>) -> Unit) : T {
  val asyncResult = awaitEvent(block)
  if (asyncResult.succeeded()) return asyncResult.result().body()
  else throw asyncResult.cause()
}

/** Converts an Either<FailureReply, T> into a return value or throws EitherFailureException if Left. */
suspend fun <T> awaitMessageEitherResult(block: (
  h: Handler<AsyncResult<Message<Either<FailureReply, T>>>>) -> Unit) : T {

  val asyncResult = awaitEvent(block)
  if (asyncResult.succeeded()) {
    val either = asyncResult.result().body()
    when (either) {
      is Either.Right -> return either.b
      is Either.Left -> throw EitherFailureException(either.a)
    }
  } else {
    throw asyncResult.cause()
  }
}

class EitherFailureException(failureReply: FailureReply)
  : RuntimeException(failureReply.toString())


suspend fun JDBCClient.getConnectionA() = awaitResult<SQLConnection> { this.getConnection(it) }

suspend fun SQLConnection.queryA(queryText: String): ResultSet =
  awaitResult { this.query(queryText, it) }

suspend fun SQLConnection.queryWithParamsA(queryText: String, params: JsonArray): ResultSet =
  awaitResult { this.queryWithParams(queryText, params, it) }

suspend fun SQLConnection.updateWithParamsA(queryText: String, params: JsonArray): UpdateResult =
  awaitResult { this.updateWithParams(queryText, params, it) }

suspend fun SQLConnection.batchWithParamsA(queryText: String, params: List<JsonArray>): List<Int> =
  awaitResult { this.batchWithParams(queryText, params, it) }

suspend fun <Failure: SerializableVertxObject, Success: SerializableVertxObject>
  CommandSender.sendA(eventBus: EventBus, message: SerializableVertxObject)
    : Message<Either<Failure, Success>> = awaitResult { this.send(eventBus, message, it) }
