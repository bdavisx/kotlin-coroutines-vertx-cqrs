package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import io.vertx.core.eventbus.*
import io.vertx.kotlin.coroutines.*
import java.time.*
import kotlin.reflect.*

internal data class SharedEventSourcedAggregateRepositoryDataSnapshot(
  val aggregateIdToLastUsedInstant: Map<AggregateId, Instant>,
  val aggregateIdToDeploymentId: Map<AggregateId, String>,
  val instantiationClassToFactory: Map<KClass<out HasAggregateVersion>, AggregateVerticleFactory>
): QueryReply

data class IsAggregateDeployedQuery(val aggregateId: AggregateId, override val correlationId: CorrelationId): Query
data class IsAggregateDeployedQueryReply(val isDeployed: Boolean): QueryReply

/** This is a fire and forget command, you won't get a reply. */
data class AddAggregateDeploymentCommand(val aggregateId: AggregateId, val deploymentId: String,
  override val correlationId: CorrelationId): DomainCommand

/** This is a fire and forget command, you won't get a reply. */
data class MarkAggregateRecentlyUsedCommand(val aggregateId: AggregateId,
  override val correlationId: CorrelationId): DomainCommand

/** This is a fire and forget command, you won't get a reply. */
/** I don't think this class can be sent across the wire. */
data class RegisterInstantiationClassesForAggregateLocalCommand(
  val factory: AggregateVerticleFactory,
  val eventClasses: List<KClass<out AggregateEvent>>,
  val snapshotClasses: List<KClass<out AggregateSnapshot>>,
  override val correlationId: CorrelationId = newId()
): DomainCommand

data class FindAggregateVerticleFactoryQuery(val kclass: KClass<out HasAggregateId>,
  override val correlationId: CorrelationId = newId()
): DomainCommand

data class FindAggregateVerticleFactoryReply(val factory: AggregateVerticleFactory?)
  : QueryReply

/** This class is expected to be a singleton for a single vertx jvm. */
class SharedEventSourcedAggregateRepositoryDataVerticle(
  private val commandRegistrar: CommandRegistrar,
  private val commandSender: CommandSender
): CoroutineVerticle() {
  companion object {
    const val maximumNumberOfCachedAggregates = 1_048_576
  }
  // TODO: probably should set the size == config.EventSourcedAggregateCacheSize + 1 for both maps
  private val aggregateIdToLastUsedInstant = mutableMapOf<AggregateId, Instant>()

  private val aggregateIdToDeploymentId = mutableMapOf<AggregateId, String>()

  private val instantiationClassToFactory =
    mutableMapOf<KClass<out HasAggregateVersion>, AggregateVerticleFactory>()

  override suspend fun start() {
    super.start()
    commandRegistrar.registerLocalCommandHandler(eventBus, IsAggregateDeployedQuery::class) {
        commandSender.reply(it, IsAggregateDeployedQueryReply(
          aggregateIdToDeploymentId.containsKey(it.body().aggregateId)))
    }
    commandRegistrar.registerLocalCommandHandler(eventBus, AddAggregateDeploymentCommand::class) {
        addAggregateDeployment(it)
    }
    commandRegistrar.registerLocalCommandHandler(eventBus, MarkAggregateRecentlyUsedCommand::class) {
        markAggregateRecentlyUsed(it)
    }
    commandRegistrar.registerLocalCommandHandler(eventBus, RegisterInstantiationClassesForAggregateLocalCommand::class) {
        addInstantiationClass(it)
    }
    commandRegistrar.registerLocalCommandHandler(eventBus, FindAggregateVerticleFactoryQuery::class) {
        findFactory(it)
    }
  }

  private fun addAggregateDeployment(commandMessage: Message<AddAggregateDeploymentCommand>) {
    val command = commandMessage.body()
    aggregateIdToDeploymentId.put(command.aggregateId, command.deploymentId)
  }

  private fun markAggregateRecentlyUsed(commandMessage: Message<MarkAggregateRecentlyUsedCommand>) {
    val command = commandMessage.body()
    aggregateIdToLastUsedInstant.put(command.aggregateId, Instant.now())
  }

  private fun addInstantiationClass(
    commandMessage: Message<RegisterInstantiationClassesForAggregateLocalCommand>) {
    val command = commandMessage.body()
    command.eventClasses.forEach { instantiationClassToFactory.put(it, command.factory) }
    command.snapshotClasses.forEach { instantiationClassToFactory.put(it, command.factory) }
  }

  private fun findFactory(commandMessage: Message<FindAggregateVerticleFactoryQuery>) {
    val command = commandMessage.body()
    commandSender.reply(commandMessage,
      FindAggregateVerticleFactoryReply(instantiationClassToFactory[command.kclass]))
  }

  internal fun snapshot() = SharedEventSourcedAggregateRepositoryDataSnapshot(
    aggregateIdToLastUsedInstant.toMap(), aggregateIdToDeploymentId.toMap(),
    instantiationClassToFactory.toMap())
}
