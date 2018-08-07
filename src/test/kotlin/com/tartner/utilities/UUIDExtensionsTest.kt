package com.tartner.utilities

import io.kotlintest.*
import org.junit.*
import java.util.*

class UUIDExtensionsTest {
  private val uuidAsString = "4d866a47-9a89-45b0-b6a0-484641e61698"

  @Test
  fun toUUID() {
    val uuid = uuidAsString.toUUID()
    val toString: String = uuid.toStringFast()
    toString shouldBe uuidAsString
  }

  @Test
  fun missingDigit() {
    val ex = shouldThrow<IllegalArgumentException> { "d866a47-9a89-45b0-b6a0-484641e61698".toUUID() }
    ex.message!!.contains("36 characters") shouldBe true
  }

  @Test
  fun badCharacter() {
    val ex = shouldThrow<IllegalArgumentException> { "4*866a47-9a89-45b0-b6a0-484641e61698".toUUID() }
    ex.message!!.contains("unexpected '*'") shouldBe true
  }

  @Test
  fun badHyphen() {
    val ex = shouldThrow<IllegalArgumentException> { "4-866a47-9a89-45b0-b6a0-484641e61698".toUUID() }
    ex.message!!.contains("unexpected '-'") shouldBe true
  }

  @Test
  fun toStringFastTest() {
    val uuid = UUID.fromString(uuidAsString)
    val toString: String = uuid.toStringFast()
    toString shouldBe uuidAsString
  }

  @Test
  fun toStringFastClearedTest() {
    val uuid = UUID.fromString(uuidAsString)
    val cleared: String = uuid.toStringFastClear()
    cleared shouldBe "4d866a479a8945b0b6a0484641e61698"
  }
}
