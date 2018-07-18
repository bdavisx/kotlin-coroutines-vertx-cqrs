package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.cqrs.*
import java.time.*
import java.util.concurrent.*
import kotlin.reflect.*

internal data class SharedEventSourcedAggregateRepositoryDataSnapshot(
  val aggregateIdToLastUsedInstant: Map<AggregateId, Instant>,
  val aggregateIdToDeploymentId: Map<AggregateId, String>,
  val instantiationClassToFactory: Map<KClass<out HasAggregateVersion>, AggregateVerticleFactory>
): QueryResponse


// TODO: we can make this a (singleton) verticle now instead of a singleton class


/** This class is expected to be a singleton for a single vertx jvm. */
class SharedEventSourcedAggregateRepositoryData {
  companion object {
    const val maximumNumberOfCachedAggregates = 1_048_576
  }
  // TODO: probably should set the size == config.EventSourcedAggregateCacheSize + 1 for both maps
  private val aggregateIdToLastUsedInstant =
    ConcurrentHashMap<AggregateId, Instant>(maximumNumberOfCachedAggregates)

  private val aggregateIdToDeploymentId =
    ConcurrentHashMap<AggregateId, String>(maximumNumberOfCachedAggregates)

  private val instantiationClassToFactory =
    ConcurrentHashMap<KClass<out HasAggregateVersion>, AggregateVerticleFactory>()

  fun isAggregateDeployed(aggregateId: AggregateId) =
    aggregateIdToDeploymentId.containsKey(aggregateId)

  fun addAggregateDeployment(aggregateId: AggregateId, deploymentId: String) =
    aggregateIdToDeploymentId.put(aggregateId, deploymentId)

  fun markAggregateRecentlyUsed(aggregateId: AggregateId) =
    aggregateIdToLastUsedInstant.put(aggregateId, Instant.now())

  fun addInstantiationClass(eventClass: KClass<out HasAggregateVersion>,
    factory: AggregateVerticleFactory) = instantiationClassToFactory.put(eventClass, factory)

  fun findFactory(clazz: KClass<out HasAggregateId>): AggregateVerticleFactory? =
    instantiationClassToFactory[clazz]

  internal fun snapshot() = SharedEventSourcedAggregateRepositoryDataSnapshot(
    aggregateIdToLastUsedInstant.toMap(), aggregateIdToDeploymentId.toMap(),
    instantiationClassToFactory.toMap())
}
