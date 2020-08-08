package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.cqrs.AggregateId

/**
 This interface is designed to be used by the EventSourcedAggregateCommandHandlerVerticle. It's not
 designed for "general" use.

 It's not like the AddressStrategy interface either, this is about distributed aggregates.
 */
interface AggregateAddressStrategy {
  fun isAggregateLocal(aggregateId: AggregateId): Boolean

  /**
   This should resolve to the "direct" local address if the aggregate is local, otherwise it should
   resolve to the remote command handler address so the remote aggregate can be loaded if necessary.
   */
  fun determineAggregateAddress(aggregateId: AggregateId): String
}

/** Simple strategy where all addresses are local. */
class DefaultAggregateAddressStrategy(private val addressPrefix: String): AggregateAddressStrategy {
  override fun isAggregateLocal(aggregateId: AggregateId): Boolean = true

  override fun determineAggregateAddress(aggregateId: AggregateId): String =
    "$addressPrefix::$aggregateId"
}
