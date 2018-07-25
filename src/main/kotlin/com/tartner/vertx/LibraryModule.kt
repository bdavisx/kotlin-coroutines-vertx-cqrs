package com.tartner.vertx

import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.cqrs.eventsourcing.*
import com.tartner.vertx.kodein.*
import org.kodein.di.*
import org.kodein.di.generic.*
import org.nustaq.serialization.*
import java.util.*

private const val defaultNodeId = "local"

// TODO: need to make sure some of these shouldn't be set by the end user
typealias UUIDGenerator=() -> UUID

val libraryModule = Kodein.Module {
  constant("nodeId") with defaultNodeId

  // TODO: the 8 here is the Max # of verticles instances to deploy, so it needs to be a config value
  bind<VerticleDeployer>() with singleton { VerticleDeployer(8, kodein) }
  bind<FSTConfiguration>() with singleton { FSTConfiguration.createDefaultConfiguration() }
  bind<TypedObjectMapper>() with singleton { TypedObjectMapper.default }
  bind<ExternalObjectMapper>() with singleton { ExternalObjectMapper.default }

  bind<KotlinSerializer>() with singleton { FSTConfigurationKotlinSerializer(i()) }

  bind<CommandSender>() with singleton { CommandSender() }
  bind<CommandRegistrar>() with singleton { CommandRegistrar(defaultNodeId) }

  bind<EventPublisher>() with singleton { EventPublisher(i()) }
  bind<EventRegistrar>() with singleton { EventRegistrar() }

  bind<SharedEventSourcedAggregateRepositoryData>() with singleton { SharedEventSourcedAggregateRepositoryData() }

  bind<EventSourcedAggregateDataVerticle>() with factory { it: String -> EventSourcedAggregateDataVerticle(it, i(), i()) }
  bind<EventSourcedAggregateRepositoryVerticle>() with provider { EventSourcedAggregateRepositoryVerticle(i(), i(), i(), i()) }
}
