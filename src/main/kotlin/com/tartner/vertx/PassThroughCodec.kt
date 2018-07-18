package com.tartner.vertx

import io.vertx.core.buffer.*
import io.vertx.core.eventbus.*

class PassThroughCodec<T>(val name: String): MessageCodec<T, T> {
  companion object {
    const val errorMessage = "Can't send code across the wire!"
  }

  override fun transform(s: T): T = s

  override fun systemCodecID(): Byte = -1
  override fun name(): String = name

  override fun encodeToWire(buffer: Buffer?, s: T?) {
    throw UnsupportedOperationException(errorMessage)
  }
  override fun decodeFromWire(pos: Int, buffer: Buffer?): T {
    throw UnsupportedOperationException(errorMessage)
  }
}
