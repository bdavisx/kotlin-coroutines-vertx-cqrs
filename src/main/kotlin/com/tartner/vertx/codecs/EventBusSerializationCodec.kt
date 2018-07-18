package com.tartner.vertx.codecs

import com.fasterxml.jackson.annotation.*
import io.vertx.core.buffer.*
import io.vertx.core.eventbus.*
import java.io.*

/** Marker interface for objects that the below codec can serialize. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
interface SerializableVertxObject: Serializable

class EventBusSerializationCodec(private val serializer: KotlinSerializer): MessageCodec<Any, Any> {
  companion object {
    val codecName = EventBusSerializationCodec::class.qualifiedName!!
  }

  override fun systemCodecID(): Byte = -1
  override fun name(): String = codecName
  override fun transform(s: Any): Any = s

  override fun encodeToWire(buffer: Buffer, value: Any) {
    val bytes = serializer.asByteArray(value)
    buffer.appendInt(bytes.size)
    buffer.appendBytes(bytes)
  }

  override fun decodeFromWire(initialPosition: Int, buffer: Buffer): Any {
    val size = buffer.getInt(initialPosition)
    val bytesPosition = initialPosition + 4
    val bytes = buffer.getBytes(bytesPosition, bytesPosition + size)

    return serializer.getObjectInput(bytes).readObject()
  }
}
