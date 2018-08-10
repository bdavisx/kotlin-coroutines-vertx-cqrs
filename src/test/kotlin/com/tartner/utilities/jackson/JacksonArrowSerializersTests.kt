package com.tartner.utilities.jackson

import arrow.core.*
import com.fasterxml.jackson.module.kotlin.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.functional.*
import io.kotlintest.*
import org.junit.*
import java.time.*

data class TestCreateTrialUserCommand(val email: String, val networkAddress: String,
  val date: OffsetDateTime, override val correlationId: CorrelationId = newId()
): DomainCommand

class JacksonArrowSerializersTest {
  @Test
  fun testEitherSerialization() {
    val left = DummyCommand(1).createLeft()
    val right = DummyCommand(0).createRight()

    val mapper = TypedObjectMapper()

    val leftSerialized = mapper.writeValueAsString(left)
    val rightSerialized = mapper.writeValueAsString(right)

    val left2 = mapper.readValue<Any>(leftSerialized)
    val right2 = mapper.readValue<Any>(rightSerialized)

    left2 shouldBe left
    right2 shouldBe right
  }

  /** I think I've decided to not use Option and go w/ Kotlin "null handling". */
  @Test
  fun testOptionSerialization() {
    val some = Some(DummyCommand(1))
    val none = None

    val mapper = TypedObjectMapper()

    val someSerialized = mapper.writeValueAsString(some)
    val noneSerialized = mapper.writeValueAsString(none)

    val some2 = mapper.readValue<Any>(someSerialized)
    val none2 = mapper.readValue<Any>(noneSerialized)

    some2 shouldBe some
    none2 shouldBe none

  }

  @Test
  fun serializeDeserialize() {
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

  @Test
  fun eitherRight() {
    val serializer = createSerializer()

    val command = Either.Right(
      TestCreateTrialUserCommand("a@b.com", "1.2.3.4", OffsetDateTime.now()))
    val json = serializer.writeValueAsString(command)
    println(json.toString())
    val deserialized = serializer.readValue<Either<*, *>>(json)
    println(deserialized)
  }

  @Test
  fun eitherLeft() {
    val serializer = createSerializer()

    val left = Either.Left(RuntimeException("Error Message"))
    val json = serializer.writeValueAsString(left)
    println(json.toString())
    val deserialized = serializer.readValue<Either<*, *>>(json)
    println(deserialized)
  }

  @Test
  fun optionSome() {
    val serializer = createSerializer()

    val command = Some(TestCreateTrialUserCommand("a@b.com", "1.2.3.4", OffsetDateTime.now()))
    val json = serializer.writeValueAsString(command)
    println(json.toString())
    val deserialized = serializer.readValue<Any>(json)
    println(deserialized)
  }

  @Test
  fun optionNone() {
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
