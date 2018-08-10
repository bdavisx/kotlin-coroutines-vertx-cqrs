package com.tartner.utilities.jackson

import arrow.core.*
import com.fasterxml.jackson.module.kotlin.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import com.tartner.vertx.functional.*
import io.kotlintest.*
import org.junit.*

data class TestCreateTrialUserCommand(val email: String, val networkAddress: String,
  override val correlationId: CorrelationId = newId()): DomainCommand

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

    val command = TestCreateTrialUserCommand("a@b.com", "1.2.3.4")
    val json = serializer.writeValueAsString(command)
    val deserialized = serializer.readValue<SerializableVertxObject>(json)
    deserialized shouldBe command
  }

  @Test
  fun eitherRight() {
    val serializer = createSerializer()

    val command = TestCreateTrialUserCommand("a@b.com", "1.2.3.4")
    val json = serializer.writeValueAsString(command.createRight())
    val deserialized = serializer.readValue<Either<*, *>>(json)
    (deserialized as Either.Right).b shouldBe  command
  }

  @Test
  fun eitherLeft() {
    val serializer = createSerializer()

    val message = "Error Message"
    val json = serializer.writeValueAsString(Either.Left(RuntimeException(message)))

    val deserialized = serializer.readValue<Either<*, *>>(json)
    ((deserialized as Either.Left).a as Exception).message shouldBe message
  }

  @Test
  fun optionSome() {
    val serializer = createSerializer()

    val command = TestCreateTrialUserCommand("a@b.com", "1.2.3.4")
    val someCommand = Some(command)
    val json = serializer.writeValueAsString(someCommand)

    val deserialized = serializer.readValue<Any>(json)
    (deserialized as Some<*>).t shouldBe command
  }

  @Test
  fun optionNone() {
    val serializer = createSerializer()

    val command = None
    val json = serializer.writeValueAsString(command)

    val deserialized = serializer.readValue<Any>(json)
    deserialized shouldBe command
  }

  private fun createSerializer(): TypedObjectMapper {
    return TypedObjectMapper()
  }
}
