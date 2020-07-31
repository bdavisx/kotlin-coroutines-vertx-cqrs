package com.tartner.vertx

import io.kotest.matchers.shouldBe
import org.junit.Test

class DequeForTestingTest {
  @Test
  fun emptyTest() {
    val deque = DequeForTesting<Any>()
    deque.isEmpty shouldBe  true
  }

  @Test
  fun threeItems() {
    val deque = DequeForTesting<Int>()
    deque.add(1)
    deque.add(2)
    deque.add(3)

    deque.get() shouldBe 1
    deque.get() shouldBe 2
    deque.get() shouldBe 3
  }

  @Test
  fun iterableFold() {
    val deque = DequeForTesting<Int>()
    deque.add(1)
    deque.add(2)
    deque.add(3)

    6 shouldBe deque.fold(0) { r, t -> r + t }
  }

  @Test
  fun emptyToString() {
    val toString = DequeForTesting.Node(1).toString()
    "Node(value=1, next=null, previous=null)" shouldBe toString
  }
}
