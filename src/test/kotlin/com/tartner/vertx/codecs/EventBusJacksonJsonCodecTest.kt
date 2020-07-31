package com.tartner.vertx.codecs

import io.kotest.matchers.shouldBe
import io.vertx.core.buffer.Buffer
import io.vertx.core.buffer.impl.BufferFactoryImpl
import org.junit.Test
import java.util.UUID

data class Test1(val id: UUID, val name: String): SerializableVertxObject
data class Test2(val id: Long, val address: String, val test1: Test1): SerializableVertxObject

class EventBusJacksonJsonCodecTest {
  @Test
  fun encodeDecodeSimpleClass() {
    val mapper = TypedObjectMapper()
    val codec = EventBusJacksonJsonCodec(mapper)

    val buffer: Buffer = BufferFactoryImpl().buffer()!!
    val test1 = Test1(UUID.randomUUID(), "Test1")

    codec.encodeToWire(buffer, test1)

    println(buffer)
    val rawDecode: Any = codec.decodeFromWire(0, buffer)
    val test1a: Test1 = rawDecode as Test1
    println(test1a)
    test1a shouldBe test1
  }

  @Test
  fun encodeDecodeAComplexClassThatContainsOtherClasses() {
    val mapper = TypedObjectMapper()
    val codec = EventBusJacksonJsonCodec(mapper)

    val buffer: Buffer = BufferFactoryImpl().buffer()!!
    val test1 = Test1(UUID.randomUUID(), "Test1")
    val test2 = Test2(123L, "Test2", test1)

    codec.encodeToWire(buffer, test2)

    println(buffer.toString())

    val test2a = codec.decodeFromWire(0, buffer)
    println(test2a)
    test2a shouldBe test2
  }
}

