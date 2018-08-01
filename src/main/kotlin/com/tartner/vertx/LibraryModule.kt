package com.tartner.vertx

import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.cqrs.eventsourcing.*
import com.tartner.vertx.kodein.*
import org.kodein.di.*
import org.kodein.di.bindings.*
import org.kodein.di.generic.*
import java.util.*

private const val defaultNodeId = "local"

// TODO: need to make sure some of these shouldn't be set by the end user
typealias UUIDGenerator=() -> UUID

val libraryModule = Kodein.Module("kotlin-coroutines-vertx-cqrs module") {
  constant("nodeId") with defaultNodeId

  // TODO: the 8 here is the Max # of verticles instances to deploy, so it needs to be a config value
  bind<VerticleKodeinProvider>() with singleton { VerticleKodeinProvider(8) }

  bind<VerticleDeployer>() with singleton { VerticleDeployer(kodein) }
  bind<TypedObjectMapper>() with singleton { TypedObjectMapper.default }
  bind<ExternalObjectMapper>() with singleton { ExternalObjectMapper.default }

  bind<CommandSender>() with singleton { CommandSender() }
  bind<CommandRegistrar>() with singleton { CommandRegistrar(defaultNodeId) }

  bind<EventPublisher>() with singleton { EventPublisher(i()) }
  bind<EventRegistrar>() with singleton { EventRegistrar() }

  bind<SharedEventSourcedAggregateRepositoryData>() with singleton { SharedEventSourcedAggregateRepositoryData() }

  bind<EventSourcedAggregateDataVerticle>() with provider { EventSourcedAggregateDataVerticle(i(), i()) }
  bind<EventSourcedAggregateRepositoryVerticle>() with provider { EventSourcedAggregateRepositoryVerticle(i(), i(), i(), i()) }
}
