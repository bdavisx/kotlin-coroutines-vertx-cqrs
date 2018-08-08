package com.tartner.utilities.jackson

import arrow.core.*
import com.fasterxml.jackson.module.kotlin.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.functional.*
import io.kotlintest.*
import org.junit.*

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
}
