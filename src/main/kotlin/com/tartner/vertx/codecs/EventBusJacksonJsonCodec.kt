package com.tartner.vertx.codecs

import com.fasterxml.jackson.annotation.*
import io.vertx.core.buffer.*
import io.vertx.core.eventbus.*

/** Marker interface for objects that the below codec can serialize. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
interface SerializableVertxObject

/**
This SHOULD NOT be used for untrusted input (like web submissions/REST) and only used when the
sending code is known.

This class simply uses jackson to serialize/deserialize objects. Jackson s/b
setup to store classes.
 */
class EventBusJacksonJsonCodec(private val mapper: TypedObjectMapper):
  MessageCodec<Any, Any> {
  companion object {
    val codecName = EventBusJacksonJsonCodec::class.qualifiedName!!
  }

  override fun systemCodecID(): Byte = -1
  override fun name(): String = codecName
  override fun transform(s: Any): Any = s

  override fun encodeToWire(buffer: Buffer, value: Any) {
    val json = mapper.writeValueAsString(value)
    buffer.appendInt(json.length)
    buffer.appendString(json)
  }

  override fun decodeFromWire(initialPosition: Int, buffer: Buffer): Any {
    val size = buffer.getInt(initialPosition)
    val jsonPosition = initialPosition + 4
    val json = buffer.getString(jsonPosition, jsonPosition + size)

    @Suppress("UNCHECKED_CAST") return mapper.readValue(json, Any::class.java)
  }
}
