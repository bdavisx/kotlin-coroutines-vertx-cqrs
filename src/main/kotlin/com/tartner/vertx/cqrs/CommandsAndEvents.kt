package com.tartner.vertx.cqrs

import com.tartner.vertx.codecs.*
import com.tartner.vertx.functional.*
import java.util.*
import kotlin.reflect.*

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
interface DomainCommand: SerializableVertxObject
interface AggregateCommand: DomainCommand, HasAggregateId
interface CommandResponse: SerializableVertxObject

interface Query: SerializableVertxObject
interface QueryResponse: SerializableVertxObject

interface AggregateSnapshot: SerializableVertxObject, HasAggregateVersion

object SuccessReply: SerializableVertxObject
val successReplyRight = SuccessReply.createRight()

interface FailureReply: SerializableVertxObject
data class ErrorReply(val message: String, val sourceClass: KClass<*>): FailureReply
