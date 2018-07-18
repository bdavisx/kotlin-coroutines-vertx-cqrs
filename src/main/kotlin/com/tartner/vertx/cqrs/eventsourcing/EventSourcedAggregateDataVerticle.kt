package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.fasterxml.jackson.module.kotlin.*
import com.tartner.vertx.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.database.*
import com.tartner.vertx.functional.*
import io.vertx.core.json.*
import io.vertx.core.logging.*
import io.vertx.ext.jdbc.*
import io.vertx.ext.sql.*
import io.vertx.kotlin.core.json.*
import org.intellij.lang.annotations.*

data class UnableToStoreAggregateEventsCommandFailure(override val message: String,
  val aggregateId: AggregateId, val events: List<DomainEvent>,
  val source: Either<*,*>? = null): GeneralCommandFailure

class EventSourcedAggregateDataVerticle(
  localAddress: String,
  private val databaseClientFactory: EventSourcingClientFactory,
  private val databaseMapper: TypedObjectMapper
): DirectCallVerticle(localAddress) {
  companion object {
    private const val valuesReplacementText = "***REPLACE_WITH_VALUES***"

    @Language("PostgreSQL")
    private val selectSnapshotSqlRaw = """
      select data
      from theSchema.snapshots
      where aggregate_id = ?
      order by version_number desc
      limit 1""".trimIndent()

    @Language("PostgreSQL")
    private val selectEventsSqlRaw = """
      select data
      from theSchema.events
      where aggregate_id = ? and version_number >= ?
      order by version_number
      """.trimIndent()

    @Language("PostgreSQL")
    private val insertEventsSqlRaw = """
      insert into theSchema.events (aggregate_id, version_number, data)
      values $valuesReplacementText""".trimIndent()

    @Language("PostgreSQL")
    private val insertSnapshotSqlRaw = """
      insert into event_sourcing.snapshots (aggregate_id, version_number, data)
      values (?, ?, cast(? as json))""".trimIndent()
  }
  private val log = LoggerFactory.getLogger(EventSourcedAggregateDataVerticle::class.java)

  private lateinit var selectSnapshotSql: String
  private lateinit var selectEventsSql: String
  private lateinit var insertEventsSql: String
  private lateinit var insertSnapshotSql: String

  private lateinit var databaseClient: JDBCClient

  override suspend fun start() {
    super.start()
    databaseClient = databaseClientFactory.create(vertx, config)

    selectEventsSql = databaseClientFactory.replaceSchema(selectEventsSqlRaw)
    selectSnapshotSql = databaseClientFactory.replaceSchema(selectSnapshotSqlRaw)
    insertEventsSql = databaseClientFactory.replaceSchema(insertEventsSqlRaw)
    insertSnapshotSql = databaseClientFactory.replaceSchema(insertSnapshotSqlRaw)
  }

  suspend fun loadAggregateEvents(aggregateId: AggregateId, aggregateVersion: Long) = actAndReply {
    try {
      // TODO: error handling
      val connection = databaseClient.getConnectionA()

      val parameters = json { array(aggregateId, aggregateVersion) }
      log.debugIf { "Running event load sql: '$selectEventsSql' with parameters: $parameters" }

      val eventsResultSet = connection.queryWithParamsA(selectEventsSql, parameters)

      val results: List<JsonArray> = eventsResultSet.results
      val events = results.map { databaseMapper.readValue<AggregateEvent>(it.getString(0)) }
      Either.Right(events)
    } catch (ex: Throwable) {
      CommandFailedDueToException(ex).createLeft()
    }
  }

  suspend fun loadLatestAggregateSnapshot(aggregateId: AggregateId) = actAndReply {
    try {
      // TODO: error handling
      val connection = databaseClient.getConnectionA()

      val parameters = json { array(aggregateId) }
      log.debugIf { "Running snapshot load sql: '$selectSnapshotSql' with parameters: $parameters" }
      val snapshotResultSet =
        connection.queryWithParamsA(selectSnapshotSql, parameters)

      val results: List<JsonArray> = snapshotResultSet.results
      val possibleSnapshot: AggregateSnapshot? = results.map {
        databaseMapper.readValue<AggregateSnapshot>(it.getString(0)) }.firstOrNull()
      Either.Right(possibleSnapshot)
    } catch (ex: Throwable) {
      CommandFailedDueToException(ex).createLeft()
    }
  }

  // TODO: where do we put the retry logic? Here or a higher level? And should it be a
  // circuit breaker? (probably should)
  suspend fun storeAggregateEvents(aggregateId: AggregateId, events: List<AggregateEvent>) =
    actAndReply {
      try {
        val numberOfEvents = events.size

        val eventsValues = json {
          array(events.flatMap({ event: AggregateEvent ->
            val eventSerialized = databaseMapper.writeValueAsString(event)
            listOf(event.aggregateId, event.aggregateVersion, eventSerialized)
          }))
        }
        val eventsParametersText = "(?, ?, cast(? as json)), ".repeat(numberOfEvents).removeSuffix(", ")
        val insertSql = insertEventsSql.replace(valuesReplacementText, eventsParametersText)
        log.debugIf { "Insert Events SQL: ***\n$insertSql\n*** with parameters $eventsValues" }

        val connection = databaseClient.getConnectionA()
        val updateResult: UpdateResult = connection.updateWithParamsA(insertSql, eventsValues)
        if (updateResult.updated != numberOfEvents) {
          ErrorReply("""
            The number of records updated (${updateResult.updated}) was not the same  as the number
            of events ($numberOfEvents) for call""".trimIndent(), this::class).createLeft()
        } else {
          successReplyRight
        }
      } catch (ex: Throwable) {
        CommandFailedDueToException(ex).createLeft()
      }
    }

  suspend fun storeAggregateSnapshot(snapshot: AggregateSnapshot) = actAndReply {
    try {
      val snapshotSerialized = databaseMapper.writeValueAsString(snapshot)
      val snapshotValues = json {
        array(snapshot.aggregateId, snapshot.aggregateVersion, snapshotSerialized)
      }

      log.debugIf {
        "Insert Snapshot SQL: ***\n$insertSnapshotSql\n*** with parameters $snapshotValues" }
      val connection = databaseClient.getConnectionA()
      val updateResult: UpdateResult =
        connection.updateWithParamsA(insertSnapshotSql, snapshotValues)
      if (updateResult.updated == 0) {
        ErrorReply("Unable to store aggregate snapshot for snapshot $snapshot", this::class)
          .createLeft()
      } else {
        successReplyRight
      }
    } catch (ex: Throwable) {
      CommandFailedDueToException(ex).createLeft()
    }
  }
}
