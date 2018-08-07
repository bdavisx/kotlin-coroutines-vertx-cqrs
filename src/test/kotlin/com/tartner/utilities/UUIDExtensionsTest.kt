package com.tartner.utilities

import io.kotlintest.*
import org.junit.*
import java.util.*

class UUIDExtensionsTest {
  @Test
  fun toUUID() {
    val uuid = "4d866a47-9a89-45b0-b6a0-484641e61698".toUUID()
    val toString: String = uuid.toStringFast()
    toString shouldBe "4d866a47-9a89-45b0-b6a0-484641e61698"
  }

  @Test
  fun toStringFastTest() {
    val uuid = UUID.fromString("4d866a47-9a89-45b0-b6a0-484641e61698")
    val toString: String = uuid.toStringFast()
    toString shouldBe "4d866a47-9a89-45b0-b6a0-484641e61698"
  }

  @Test
  fun toStringFastClearedTest() {
    val uuid = UUID.fromString("4d866a47-9a89-45b0-b6a0-484641e61698")
    val cleared: String = uuid.toStringFastClear()
    cleared shouldBe "4d866a479a8945b0b6a0484641e61698"
  }
}
