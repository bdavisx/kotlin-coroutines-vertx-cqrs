package com.tartner.vertx.cqrs

import com.tartner.vertx.codecs.SerializableVertxObject
import com.tartner.vertx.functional.createRight
import java.util.UUID
import kotlin.reflect.KClass

interface HasAggregateId {
  val aggregateId: UUID
}

interface HasAggregateVersion: HasAggregateId {
  val aggregateVersion: Long
}

annotation class EventHandler
interface DomainEvent: SerializableVertxObject

interface AggregateEvent: DomainEvent, HasAggregateVersion

/** This indicates an error happened that needs to be handled at a higher/different level. */
interface ErrorEvent: DomainEvent

annotation class CommandHandler

typealias CommandId = UUID
interface DomainCommand: SerializableVertxObject {
  val commandId: CommandId
}
open class DefaultDomainCommand(override val commandId: CommandId = UUID.randomUUID()): DomainCommand

interface AggregateCommand: DomainCommand, HasAggregateId
class DefaultAggregateCommand(override val aggregateId: AggregateId)
  : DefaultDomainCommand(), AggregateCommand

interface CommandResponse: SerializableVertxObject

interface Query: SerializableVertxObject
interface QueryResponse: SerializableVertxObject

interface AggregateSnapshot: SerializableVertxObject, HasAggregateVersion

object SuccessReply: SerializableVertxObject
val successReplyRight = SuccessReply.createRight()

interface FailureReply: SerializableVertxObject
data class ErrorReply(val message: String, val sourceClass: KClass<*>): FailureReply
