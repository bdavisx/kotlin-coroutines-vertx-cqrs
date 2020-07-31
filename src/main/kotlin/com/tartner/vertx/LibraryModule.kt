package com.tartner.vertx

import com.tartner.vertx.codecs.ExternalObjectMapper
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.commands.CommandRegistrar
import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.commands.EventRegistrar
import com.tartner.vertx.cqrs.EventPublisher
import com.tartner.vertx.cqrs.eventsourcing.EventSourcedAggregateDataVerticle
import com.tartner.vertx.cqrs.eventsourcing.EventSourcedAggregateRepositoryVerticle
import com.tartner.vertx.cqrs.eventsourcing.SharedEventSourcedAggregateRepositoryData
import com.tartner.vertx.kodein.VerticleDeployer
import com.tartner.vertx.kodein.VerticleKodeinProvider
import com.tartner.vertx.kodein.i
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import org.kodein.di.generic.with
import java.util.UUID

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
