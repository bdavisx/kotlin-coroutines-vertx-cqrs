package com.tartner.vertx.cqrs.eventsourcing

import arrow.core.*
import com.fasterxml.jackson.module.kotlin.*
import com.tartner.vertx.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.database.*
import com.tartner.vertx.functional.*
import io.vertx.core.Handler
import io.vertx.core.eventbus.*
import io.vertx.core.json.*
import io.vertx.core.logging.*
import io.vertx.ext.jdbc.*
import io.vertx.ext.sql.*
import io.vertx.kotlin.core.json.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.experimental.*
import org.intellij.lang.annotations.*

data class StoreAggregateEventsCommand(
  val aggregateId: AggregateId, val events: List<AggregateEvent>): DomainCommand by DefaultDomainCommand()

data class UnableToStoreAggregateEventsCommandFailure(override val message: String,
  val aggregateId: AggregateId, val events: List<DomainEvent>,
  val source: Either<*,*>? = null): GeneralCommandFailure

data class StoreAggregateSnapshotCommand(val snapshot: AggregateSnapshot)
  : DomainCommand by DefaultDomainCommand()

data class LoadAggregateEventsCommand(override val aggregateId: AggregateId,
  val aggregateVersion: Long): AggregateCommand, DomainCommand by DefaultDomainCommand()

data class LoadLatestAggregateSnapshotCommand(override val aggregateId: AggregateId)
  : AggregateCommand, DomainCommand by DefaultDomainCommand()

class EventSourcedAggregateDataVerticle(
  private val commandSender: CommandSender,
  private val commandRegistrar: CommandRegistrar,
  private val databaseClientFactory: EventSourcingClientFactory,
  private val databaseMapper: TypedObjectMapper
): CoroutineVerticle() {
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
    databaseClient = databaseClientFactory.create(vertx, config)

    selectEventsSql = databaseClientFactory.replaceSchema(selectEventsSqlRaw)
    selectSnapshotSql = databaseClientFactory.replaceSchema(selectSnapshotSqlRaw)
    insertEventsSql = databaseClientFactory.replaceSchema(insertEventsSqlRaw)
    insertSnapshotSql = databaseClientFactory.replaceSchema(insertSnapshotSqlRaw)

    commandRegistrar.registerLocalCommandHandler(eventBus, LoadAggregateEventsCommand::class,
      Handler { launch(vertx.dispatcher()) { loadEvents(it) } })

    commandRegistrar.registerLocalCommandHandler(eventBus, LoadLatestAggregateSnapshotCommand::class,
      Handler { launch(vertx.dispatcher()) { loadSnapshot(it) } })

    commandRegistrar.registerLocalCommandHandler(eventBus, StoreAggregateEventsCommand::class,
      Handler { launch(vertx.dispatcher()) { storeEvents(it) } })

    commandRegistrar.registerLocalCommandHandler(eventBus, StoreAggregateSnapshotCommand::class,
      Handler { launch(vertx.dispatcher()) { storeSnapshot(it) } })
  }

  private suspend fun loadEvents(commandMessage: Message<LoadAggregateEventsCommand>) {
    val command = commandMessage.body()
    try {
      // TODO: error handling
      val connection = databaseClient.getConnectionA()

      val parameters = json { array(command.aggregateId, command.aggregateVersion) }
      log.debugIf { "Running event load sql: '$selectEventsSql' with parameters: $parameters" }

      val eventsResultSet = connection.queryWithParamsA(selectEventsSql, parameters)

      val results: List<JsonArray> = eventsResultSet.results
      val events = results.map { databaseMapper.readValue<AggregateEvent>(it.getString(0)) }
      commandSender.reply(commandMessage, Either.Right(events))
    } catch (ex: Throwable) {
      commandSender.reply(commandMessage, CommandFailedDueToException(ex).createLeft())
    }
  }

  private suspend fun loadSnapshot(commandMessage: Message<LoadLatestAggregateSnapshotCommand>) {
    val command = commandMessage.body()
    try {
      // TODO: error handling
      val connection = databaseClient.getConnectionA()

      val parameters = json { array(command.aggregateId) }
      log.debugIf { "Running snapshot load sql: '$selectSnapshotSql' with parameters: $parameters" }
      val snapshotResultSet =
        connection.queryWithParamsA(selectSnapshotSql, parameters)

      val results: List<JsonArray> = snapshotResultSet.results
      val possibleSnapshot: AggregateSnapshot? = results.map {
        databaseMapper.readValue<AggregateSnapshot>(it.getString(0)) }.firstOrNull()
      commandSender.reply(commandMessage, Either.Right(possibleSnapshot))
    } catch (ex: Throwable) {
      commandSender.reply(commandMessage, CommandFailedDueToException(ex).createLeft())
    }
  }

  // TODO: where do we put the retry logic? Here or a higher level? And should it be a
  // circuit breaker? (probably should)
  private suspend fun storeEvents(commandMessage: Message<StoreAggregateEventsCommand>) {
    val command = commandMessage.body()
      try {
      val numberOfEvents = command.events.size

        val eventsValues = json {
        array(command.events.flatMap({ event: AggregateEvent ->
            val eventSerialized = databaseMapper.writeValueAsString(event)
            listOf(event.aggregateId, event.aggregateVersion, eventSerialized)
          }))
        }
      val eventsParametersText =
        "(?, ?, cast(? as json)), ".repeat(numberOfEvents).removeSuffix(", ")
        val insertSql = insertEventsSql.replace(valuesReplacementText, eventsParametersText)
        log.debugIf { "Insert Events SQL: ***\n$insertSql\n*** with parameters $eventsValues" }

        val connection = databaseClient.getConnectionA()
        val updateResult: UpdateResult = connection.updateWithParamsA(insertSql, eventsValues)
        if (updateResult.updated != numberOfEvents) {
        commandSender.reply(commandMessage, ErrorReply("""
            The number of records updated (${updateResult.updated}) was not the same  as the number
            of events ($numberOfEvents) for command $command""".trimIndent(),
          this::class).createLeft())
        } else {
        commandSender.reply(commandMessage, successReplyRight)
        }
      } catch (ex: Throwable) {
      commandSender.reply(commandMessage, CommandFailedDueToException(ex).createLeft())
      }
    }

  private suspend fun storeSnapshot(commandMessage: Message<StoreAggregateSnapshotCommand>) {
    val command = commandMessage.body()
    val snapshot = command.snapshot
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
        commandSender.reply(commandMessage,
          ErrorReply("Unable to store aggregate snapshot for command $command", this::class)
            .createLeft())
      } else {
        commandSender.reply(commandMessage, successReplyRight)
      }
    } catch (ex: Throwable) {
      commandSender.reply(commandMessage, CommandFailedDueToException(ex).createLeft())
    }
  }
}
