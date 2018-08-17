package com.tartner.vertx.codecs

import io.kotlintest.*
import io.vertx.core.buffer.*
import io.vertx.core.buffer.impl.*
import io.vertx.ext.unit.junit.*
import org.junit.*
import org.junit.runner.*
import java.util.*

data class Data1(val id: UUID, val name: String): SerializableVertxObject
data class Data2(val id: Long, val address: String, val data1: Data1): SerializableVertxObject

class EventBusJacksonJsonCodecTest() {
  @Test
  fun encodeDecodeSimpleClass() {
    val mapper = TypedObjectMapper()
    val codec = EventBusJacksonJsonCodec(mapper)

    val buffer: Buffer = BufferFactoryImpl().buffer()!!
    val test1 = Data1(UUID.randomUUID(), "Test1")

    codec.encodeToWire(buffer, test1)

    println(buffer)
    val rawDecode: Any = codec.decodeFromWire(0, buffer)
    val data1A: Data1 = rawDecode as Data1
    println(data1A)
    data1A shouldBe test1
  }

  @Test
  fun encodeDecodeComplexClassContainingOtherClasses() {
    val mapper = TypedObjectMapper()
    val codec = EventBusJacksonJsonCodec(mapper)

    val buffer: Buffer = BufferFactoryImpl().buffer()!!
    val test1 = Data1(UUID.randomUUID(), "Test1")
    val test2 = Data2(123L, "Test2", test1)

    codec.encodeToWire(buffer, test2)

    println(buffer.toString())

    val test2a = codec.decodeFromWire(0, buffer)
    println(test2a)
    test2a shouldBe test2
  }
}
