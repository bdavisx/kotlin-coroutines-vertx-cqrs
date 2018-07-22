package com.tartner.vertx

interface AddressStrategy {
  /**
   * Takes an address String and returns that address with the local cluster "marker" - typically
   * a prefix or suffix.
   */
  fun addLocalAddressMarker(originalAddress: String): String

  /** Checks an address to see if it has the local cluster "marker" embedded. */
  fun isLocalAddress(address: String): Boolean
}

/** Strategy to use when there's no cluster at all. */
class NonClusteredAddressStrategy(): AddressStrategy {
  override fun addLocalAddressMarker(originalAddress: String): String = originalAddress
  override fun isLocalAddress(address: String): Boolean = true
}
