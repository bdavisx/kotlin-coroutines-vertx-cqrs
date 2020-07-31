package com.tartner.vertx.codecs

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tartner.utilities.jackson.ArrowMixIn
import com.tartner.utilities.jackson.JacksonArrowEitherLeftDeserializer
import com.tartner.utilities.jackson.JacksonArrowEitherRightDeserializer
import com.tartner.utilities.jackson.JacksonArrowOptionNoneDeserializer
import com.tartner.utilities.jackson.JacksonArrowOptionSomeDeserializer

/**
 Note: This class should only be used for "internal"/trusted serialization/deserialization. There
 are security issues around Jackson Datamapper and `enableDefaultTyping...` is part of what allows
 the exploit to happen. As long as the json is trusted, this class is safe to use. So there needs to
 be another class that is used for stuff coming in over the internet.
 */
class TypedObjectMapper: ObjectMapper() {
  companion object {
    val default = TypedObjectMapper()
  }

  init {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
    addMixIn(Either::class.java, ArrowMixIn::class.java)
    addMixIn(Option::class.java, ArrowMixIn::class.java)

    // TODO: this belongs somewhere else
    val sm = SimpleModule()
      .addDeserializer(Either.Left::class.java, JacksonArrowEitherLeftDeserializer())
      .addDeserializer(Either.Right::class.java, JacksonArrowEitherRightDeserializer())
      .addDeserializer(Some::class.java, JacksonArrowOptionSomeDeserializer())
      .addDeserializer(None::class.java, JacksonArrowOptionNoneDeserializer())
    registerModule(sm)
  }
}

/** Mapper that s/b used for reading/writing json that goes over the internet. */
class ExternalObjectMapper: ObjectMapper() {
  companion object {
    val default = ExternalObjectMapper()
  }

  init {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    disableDefaultTyping()
  }
}
