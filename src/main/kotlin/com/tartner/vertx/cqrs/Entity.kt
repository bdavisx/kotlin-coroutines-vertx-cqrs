package com.tartner.vertx.cqrs

import java.util.*

typealias AggregateId = UUID
typealias EntityId = UUID

interface Entity {
  val entityId: EntityId
}
