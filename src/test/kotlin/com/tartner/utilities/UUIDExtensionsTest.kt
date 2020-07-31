package com.tartner.utilities

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class UUIDExtensionsTest {
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
