package com.tartner.vertx.codecs

import org.nustaq.serialization.*
import java.io.*

interface KotlinSerializer {
  fun <T> asByteArray(value: T): ByteArray
  fun getObjectInput(bytes: ByteArray): ObjectInput
}

class FSTConfigurationKotlinSerializer(private val serializer: FSTConfiguration): KotlinSerializer {
  override fun <T> asByteArray(value: T): ByteArray = serializer.asByteArray(value)
  override fun getObjectInput(bytes: ByteArray): ObjectInput = serializer.getObjectInput(bytes)
}

