package com.tartner.vertx

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import com.fasterxml.jackson.module.kotlin.readValue
import com.tartner.vertx.codecs.SerializableVertxObject
import com.tartner.vertx.codecs.TypedObjectMapper
import com.tartner.vertx.cqrs.DefaultDomainCommand
import com.tartner.vertx.cqrs.DomainCommand
import org.junit.Test
import java.time.OffsetDateTime

data class TestCreateTrialUserCommand(val email: String, val networkAddress: String,
  val date: OffsetDateTime): DomainCommand by DefaultDomainCommand()

data class CommandList(val commands: List<DomainCommand>): SerializableVertxObject

class KObjectSerializerTest() {
  @Test()
  fun eitherRightSerializableJackson() {
    val serializer = createSerializer()

    val command = Either.Right(TestCreateTrialUserCommand("a@b.com", "1.2.3.4", OffsetDateTime.now()))
    val json = serializer.writeValueAsString(command)
    println(json.toString())
    val deserialized = serializer.readValue<Either<*,*>>(json)
    println(deserialized)
  }

  @Test()
  fun eitherLeftSerializableJackson() {
    val serializer = createSerializer()

    val left = Either.Left(RuntimeException("Error Message"))
    val json = serializer.writeValueAsString(left)
    println(json.toString())
    val deserialized = serializer.readValue<Either<*,*>>(json)
    println(deserialized)
  }

  @Test()
  fun optionSomeSerializableJackson() {
    val serializer = createSerializer()

    val command = Some(TestCreateTrialUserCommand("a@b.com", "1.2.3.4", OffsetDateTime.now()))
    val json = serializer.writeValueAsString(command)
    println(json.toString())
    val deserialized = serializer.readValue<Any>(json)
    println(deserialized)
  }

  @Test()
  fun optionNoneSerializableJackson() {
    val serializer = createSerializer()

    val command = None
    val json = serializer.writeValueAsString(command)
    println(json.toString())
    val deserialized = serializer.readValue<Any>(json)
    println(deserialized)
  }

  private fun createSerializer(): TypedObjectMapper {
    return TypedObjectMapper()
  }
}
