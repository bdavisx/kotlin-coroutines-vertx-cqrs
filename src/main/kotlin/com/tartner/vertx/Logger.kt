package com.tartner.vertx

import com.tartner.utilities.EmptyUUID
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.cqrs.CorrelationId
import com.tartner.vertx.cqrs.DomainCommand
import com.tartner.vertx.kodein.SpecificNumberOfVerticleInstancesToDeploy
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.io.PrintWriter
import java.time.Instant
import java.time.format.DateTimeFormatter

interface Logger {
  suspend fun error(correlationId: CorrelationId, messageProvider: () -> String)
  suspend fun error(correlationId: CorrelationId, cause: Throwable, messageProvider: () -> String)
  suspend fun warn(correlationId: CorrelationId, messageProvider: () -> String)
  suspend fun warn(correlationId: CorrelationId, cause: Throwable, messageProvider: () -> String)
  suspend fun info(correlationId: CorrelationId, messageProvider: () -> String)
  suspend fun info(correlationId: CorrelationId, cause: Throwable, messageProvider: () -> String)
  suspend fun debug(correlationId: CorrelationId, messageProvider: () -> String)
  suspend fun debug(correlationId: CorrelationId, cause: Throwable, messageProvider: () -> String)
  suspend fun trace(correlationId: CorrelationId, messageProvider: () -> String)
  suspend fun trace(correlationId: CorrelationId, cause: Throwable, messageProvider: () -> String)
}

enum class LogLevel { Error, Warn, Info, Debug, Trace }

data class LogMessage(val message: String, val loggerName: String, val level: LogLevel,
  val timestamp: Instant, override val correlationId: CorrelationId = EmptyUUID): DomainCommand
data class LogMessageWithThrowable(val message: String, val loggerName: String, val level: LogLevel,
  val timestamp: Instant, val cause: Throwable,
  override val correlationId: CorrelationId = EmptyUUID): DomainCommand

/**
 * Handles logging in a vertx message driven fashion.
 */
@SpecificNumberOfVerticleInstancesToDeploy(1)
class AsyncLogger(
  private val commandRegistrar: CommandRegistrar,
  private val outputFilePrefix: String = "./logs/app",
  private val outputFileSuffix: String = ".log"
): CoroutineVerticle() {

  companion object {
    private val logMessageInstantFormatter =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss.SSS")
  }

  private val currentOutput: PrintWriter

  init {
    currentOutput = openNextOutputFile()
  }

  // we're just going to log to the console for now :)
  private fun openNextOutputFile(): PrintWriter = PrintWriter(System.out)

  override suspend fun start() {
    super.start()
    commandRegistrar.registerCommandHandler(this, LogMessage::class, ::logMessage)
  }

  suspend fun logMessage(logEntry: LogMessage) {
    val dateTime = logMessageInstantFormatter.format(logEntry.timestamp)
    val message = "$dateTime ${logEntry.loggerName} ${logEntry.level} ${logEntry.correlationId} - ${logEntry.message}"
    currentOutput.write(message)
  }
}


/**
 * @param loggerName - The path (typically class name) to put in the log entries.
 */
class TraceLogger(
  val loggerName: String,
  val commandSender: CommandSender
): Logger {
  private fun log(correlationId: CorrelationId, logLevel: LogLevel, messageProvider: () -> String) =
    commandSender.send(
      LogMessage(messageProvider(), loggerName, logLevel, Instant.now(), correlationId))

  private fun logWithThrowable(correlationId: CorrelationId, logLevel: LogLevel, cause: Throwable,
    messageProvider: () -> String) = commandSender.send(
      LogMessage(messageProvider(), loggerName, logLevel, Instant.now(), correlationId))

  override suspend fun warn(correlationId: CorrelationId, messageProvider: () -> String) =
    log(correlationId, LogLevel.Warn, messageProvider)

  override suspend fun warn(correlationId: CorrelationId, cause: Throwable,
    messageProvider: () -> String) =
    logWithThrowable(correlationId, LogLevel.Warn, cause, messageProvider)

  override suspend fun info(correlationId: CorrelationId, messageProvider: () -> String) =
    log(correlationId, LogLevel.Info, messageProvider)

  override suspend fun info(correlationId: CorrelationId, cause: Throwable,
    messageProvider: () -> String) =
    logWithThrowable(correlationId, LogLevel.Info, cause, messageProvider)

  override suspend fun debug(correlationId: CorrelationId, messageProvider: () -> String) =
    log(correlationId, LogLevel.Debug, messageProvider)

  override suspend fun debug(correlationId: CorrelationId, cause: Throwable,
    messageProvider: () -> String) =
    logWithThrowable(correlationId, LogLevel.Debug, cause, messageProvider)

  override suspend fun trace(correlationId: CorrelationId, messageProvider: () -> String) =
    log(correlationId, LogLevel.Trace, messageProvider)

  override suspend fun trace(correlationId: CorrelationId, cause: Throwable,
    messageProvider: () -> String) =
    logWithThrowable(correlationId, LogLevel.Trace, cause, messageProvider)

  override suspend fun error(correlationId: CorrelationId, messageProvider: () -> String) =
    log(correlationId, LogLevel.Error, messageProvider)

  override suspend fun error(correlationId: CorrelationId, cause: Throwable,
    messageProvider: () -> String) =
    logWithThrowable(correlationId, LogLevel.Error, cause, messageProvider)
}
