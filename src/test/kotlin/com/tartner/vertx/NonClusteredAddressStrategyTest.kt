package com.tartner.vertx

import io.kotlintest.*
import org.junit.*

class NonClusteredAddressStrategyTest() {
  @Test
  fun nonClusteredAddressStrategy() {
    val strategy = NonClusteredAddressStrategy()

    val originalAddress = "address"

    val markedAddress = strategy.addLocalAddressMarker(originalAddress)

    markedAddress shouldBe originalAddress
    strategy.isLocalAddress(markedAddress) shouldBe true
  }
}
