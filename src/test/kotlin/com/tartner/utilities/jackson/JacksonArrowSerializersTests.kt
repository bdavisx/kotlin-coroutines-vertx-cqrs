package com.tartner.utilities.jackson

import com.fasterxml.jackson.module.kotlin.*
import com.tartner.vertx.codecs.*
import com.tartner.vertx.commands.*
import com.tartner.vertx.functional.*
import io.kotlintest.*
import org.junit.*
import org.junit.Assert.*

class JacksonArrowEitherLeftDeserializerTest {
  @Test
  fun testSerialization() {
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
}
