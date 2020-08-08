package com.tartner.vertx.cqrs

import com.tartner.vertx.codecs.SerializableVertxObject
import com.tartner.vertx.functional.createRight
import java.util.UUID
import kotlin.reflect.KClass

// MUSTFIX: Docs for these interfaces

typealias MessageHandler<T> = (T) -> Unit
typealias ReplyMessageHandler<T> = (T, Reply) -> Unit

typealias SuspendableMessageHandler<T> = suspend (T) -> Unit
typealias SuspendableReplyMessageHandler<T> = suspend (T, Reply) -> Unit

typealias Reply = (Any) -> Unit

data class AggregateId(val id: String): SerializableVertxObject
data class AggregateVersion(val version: Long)
  : Comparable<AggregateVersion>, SerializableVertxObject {
  override fun compareTo(other: AggregateVersion): Int = version.compareTo(other.version)
}

interface HasAggregateId { val aggregateId: AggregateId }

interface HasAggregateVersion: HasAggregateId, Comparable<HasAggregateVersion> {
  val aggregateVersion: AggregateVersion

  override fun compareTo(other: HasAggregateVersion): Int =
    aggregateVersion.compareTo(other.aggregateVersion)
}

annotation class EventHandler
interface DomainEvent: SerializableVertxObject, HasCorrelationId

interface AggregateEvent: DomainEvent, HasAggregateVersion

/** This indicates an error happened that needs to be handled at a higher/different level. */
interface ErrorEvent: DomainEvent

typealias CorrelationId = UUID
interface HasCorrelationId {
  val correlationId: CorrelationId
}

fun newId() = UUID.randomUUID()

annotation class CommandHandler

interface DomainCommand: SerializableVertxObject, HasCorrelationId

interface AggregateCommand: DomainCommand, HasAggregateId

interface CommandReply: SerializableVertxObject

interface Query: SerializableVertxObject, HasCorrelationId
interface QueryReply: SerializableVertxObject

interface AggregateSnapshot: SerializableVertxObject, HasAggregateVersion

object SuccessReply: SerializableVertxObject
val successReplyRight = SuccessReply.createRight()

interface FailureReply: SerializableVertxObject
data class ErrorReply(val message: String, val sourceClass: KClass<*>): FailureReply
