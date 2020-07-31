package com.tartner.vertx.database

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

/*
    By inheriting JDBCClient, we can create a Type specific class that can act as a JDBCClient. This
    allows us to actually specify a particular interface as a dependency (e.g. QueryJDBCClient,
    which is a JDBC Client specifically for the query side in CQRS.

    We don't change JDBCClient or add to it in any way. This is specifically about declaring a
    dependency on a specific database connection. The only QueryJDBCClient that will be created
    will be the one for Querying.
*/

interface JDBCClientFactory {
  fun create(vertx: Vertx, configuration: JsonObject): JDBCClient
  fun replaceSchema(sql: String): String
}

interface AuthenticationClientFactory: JDBCClientFactory
interface EventSourcingClientFactory: JDBCClientFactory
interface QueryModelClientFactory: JDBCClientFactory

val databaseFactoryModule = Kodein.Module {
  bind<AuthenticationClientFactory>() with singleton { AuthenticationJDBCClientFactory() }
  bind<EventSourcingClientFactory>() with singleton { EventSourcingJDBCClientFactory() }
  bind<QueryModelClientFactory>() with singleton { QueryModelJDBCClientFactory() }
}

abstract class AbstractJDBCClientFactory(private val schema: String): JDBCClientFactory {
  companion object {
    // TODO: should probably be configurable
    val valueToReplace: String = "theSchema"
  }

  override fun replaceSchema(sql: String): String {
    return sql.replace(valueToReplace, schema)
  }
}

class AuthenticationJDBCClientFactory: AbstractJDBCClientFactory("query_model"),
  AuthenticationClientFactory {
  override fun create(vertx: Vertx, configuration: JsonObject): JDBCClient = createClient(vertx,
    configuration, "DATABASE_QUERY_MODEL_", "Authentication")
}

class EventSourcingJDBCClientFactory: AbstractJDBCClientFactory("event_sourcing"),
  EventSourcingClientFactory {
  override fun create(vertx: Vertx, configuration: JsonObject): JDBCClient = createClient(vertx,
    configuration, "DATABASE_EVENT_SOURCING_", "EventSourcing")
}

class QueryModelJDBCClientFactory: AbstractJDBCClientFactory("query_model"),
  QueryModelClientFactory {
  override fun create(vertx: Vertx, configuration: JsonObject): JDBCClient = createClient(vertx,
    configuration, "DATABASE_QUERY_MODEL_", "QueryModel")
}

private fun createClient(vertx: Vertx, configuration: JsonObject, environmentNamePrefix: String,
  clientName: String): JDBCClient {

  val databaseConfiguration = createDatabaseConfiguration(configuration, environmentNamePrefix)
  return JDBCClient.createShared(vertx, databaseConfiguration, clientName)
}

private fun createDatabaseConfiguration(configuration: JsonObject,
  environmentNamePrefix: String): JsonObject? {
  return JsonObject()
    .put("url", configurationValue(configuration, environmentNamePrefix, "URL"))
    .put("driver_class", configurationValue(configuration, environmentNamePrefix, "CLASS"))
    .put("user", configurationValue(configuration, environmentNamePrefix, "USERID"))
    .put("password", configurationValue(configuration, environmentNamePrefix, "PASSWORD"))
}

private fun configurationValue(mainConfiguration: JsonObject, environmentNamePrefix: String,
  value: String) = mainConfiguration.getValue(prepend(environmentNamePrefix, value))

private fun prepend(pre: String, value: String) = StringBuilder(pre).append(value).toString()
