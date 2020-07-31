package com.tartner.vertx.cqrs

import java.util.UUID

typealias AggregateId = UUID
typealias EntityId = UUID

interface Entity {
  val entityId: EntityId
}
