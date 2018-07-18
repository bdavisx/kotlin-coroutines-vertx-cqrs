package com.tartner.vertx.codecs

import io.vertx.core.buffer.*
import io.vertx.core.eventbus.*
import kotlin.reflect.*

/**
This SHOULD NOT be used for untrusted input (like web submissions/REST) and only used when the
sending code is known.

This class simply uses jackson to serialize/deserialize objects. Jackson s/b
setup to store classes.
 */
class EventBusJacksonJsonCodec<T>(private val mapper: TypedObjectMapper, private val codecFor: KClass<*>):
  MessageCodec<T, T> {
  private val qualifiedClassName = codecFor.qualifiedName!!

  override fun systemCodecID(): Byte = -1
  override fun name(): String = qualifiedClassName
  override fun transform(s: T): T = s

  override fun encodeToWire(buffer: Buffer, value: T) {
    val json = mapper.writeValueAsString(value)
    buffer.appendInt(json.length)
    buffer.appendString(json)
  }

  override fun decodeFromWire(initialPosition: Int, buffer: Buffer): T {
    val size = buffer.getInt(initialPosition)
    val jsonPosition = initialPosition + 4
    val json = buffer.getString(jsonPosition, jsonPosition + size)

    // don't see a way around the unchecked cast, since we lose the T type at runtime
    @Suppress("UNCHECKED_CAST") return mapper.readValue(json, codecFor.java) as T
  }
}

