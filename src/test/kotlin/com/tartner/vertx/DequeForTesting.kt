package com.tartner.vertx

/** This is a very minimal class, not thread safe at all. */
class DequeForTesting<T>: Iterable<T> {
  class Node<T>(val value: T) {
    var next: Node<T>? = null
    var previous: Node<T>? = null

    override fun toString(): String {
      return "Node(value=$value, next=${next?.value}, previous=${previous?.value})"
    }
  }

  private class DequeIterator<T>(initial: Node<T>?): Iterator<T> {
    private var currentNode: Node<T>? = initial

    override fun hasNext(): Boolean {
      return currentNode != null
    }

    override fun next(): T {
      val current = currentNode ?: throw IllegalStateException("No elements!")

      currentNode = current.next
      return current.value
    }
  }

  private var head: Node<T>? = null
  private var tail: Node<T>? = null

  var isEmpty: Boolean = head == null

  private fun first(): Node<T>? = head
  private fun last(): Node<T>? = tail

  override fun iterator(): Iterator<T> = DequeIterator(first())

  /** Returns the first() item in the deque and removes it. Exception if empty. */
  fun get(): T {
    val head = this.head ?: throw IllegalStateException("No items in deque!")

    val newHead = head.next
    if (newHead != null) {
      newHead.previous = null
    } else {
      tail = null
    }

    this.head = newHead

    return head.value
  }

  fun add(value: T) {
    val newNode = Node(value)

    val lastNode = this.last()
    if (lastNode != null) {
      newNode.previous = lastNode
      lastNode.next = newNode
    } else {
      head = newNode
    }
    tail = newNode
  }

  fun removeAll() {
    head = null
    tail = null
  }

  override fun toString(): String {
    var s = "["
    var node = head
    while (node != null) {
      s += "${node.value}"
      node = node.next
      if (node != null) {
        s += ", "
      }
    }
    return "$s]"
  }
}
