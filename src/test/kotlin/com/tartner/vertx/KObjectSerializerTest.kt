package com.tartner.vertx

import arrow.core.*
import com.fasterxml.jackson.module.kotlin.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.cqrs.*
import io.kotlintest.specs.*
import java.time.*

data class TestCreateTrialUserCommand(val email: String, val networkAddress: String,
  val date: OffsetDateTime): DomainCommand by DefaultDomainCommand()

data class CommandList(val commands: List<DomainCommand>): SerializableVertxObject

class KObjectSerializerTest: FeatureSpec() {
  init {
    feature("Serialize/Deserialize functions") {
      scenario("Serialize/Deserialize works") {
        val serializer = TypedObjectMapper()

        val command = TestCreateTrialUserCommand("a@b.com", "1.2.3.4", OffsetDateTime.now())
        val json = serializer.writeValueAsString(command)
        println(json)
        val deserialized = serializer.readValue<SerializableVertxObject>(json)
        println(deserialized)

//        val commands = CommandList((1..10000).map {
//          CreateTrialUserCommand("a@b.com", "1.2.3.4", OffsetDateTime.now())
//        })
//        val cjson = serializer.writeValueAsString(commands)
//        serializer.readValue<SerializableVertxObject>(cjson)
//        println(cjson)
      }

      scenario("Either.right serializable Jackson") {
        val serializer = createSerializer()

        val command = Either.Right(TestCreateTrialUserCommand("a@b.com", "1.2.3.4", OffsetDateTime.now()))
        val json = serializer.writeValueAsString(command)
        println(json.toString())
        val deserialized = serializer.readValue<Either<*,*>>(json)
        println(deserialized)
      }

      scenario("Either.left serializable Jackson") {
        val serializer = createSerializer()

        val left = Either.Left(RuntimeException("Error Message"))
        val json = serializer.writeValueAsString(left)
        println(json.toString())
        val deserialized = serializer.readValue<Either<*,*>>(json)
        println(deserialized)
      }

      scenario("Options-Some serializable Jackson") {
        val serializer = createSerializer()

        val command = Some(TestCreateTrialUserCommand("a@b.com", "1.2.3.4", OffsetDateTime.now()))
        val json = serializer.writeValueAsString(command)
        println(json.toString())
        val deserialized = serializer.readValue<Any>(json)
        println(deserialized)
      }

      scenario("Options-None serializable Jackson") {
        val serializer = createSerializer()

        val command = None
        val json = serializer.writeValueAsString(command)
        println(json.toString())
        val deserialized = serializer.readValue<Any>(json)
        println(deserialized)
      }
    }
  }

  private fun createSerializer(): TypedObjectMapper {
    return TypedObjectMapper()
  }
}
