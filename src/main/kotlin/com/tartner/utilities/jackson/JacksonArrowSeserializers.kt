package com.tartner.utilities.jackson

import arrow.core.Either
import arrow.core.Some
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
interface ArrowMixIn

class JacksonArrowEitherLeftDeserializer:
  StdDeserializer<Either.Left<Any>>(Either.Left::class.java) {

  override fun deserialize(parser: JsonParser, context: DeserializationContext): Either.Left<Any> {
    val node: JsonNode = parser.codec.readTree(parser)
    val valueNode = node.get("a")
    val mapper: ObjectMapper = parser.codec as ObjectMapper
    val value = mapper.treeToValue<Any>(valueNode, Any::class.java)
    return Either.left(value) as Either.Left<Any>
  }

}

class JacksonArrowEitherRightDeserializer:
  StdDeserializer<Either.Right<Any>>(Either::class.java) {

  override fun deserialize(parser: JsonParser, context: DeserializationContext): Either.Right<Any> {
    val node: JsonNode = parser.codec.readTree(parser)
    val valueNode = node.get("b")
    val mapper: ObjectMapper = parser.codec as ObjectMapper
    val value = mapper.treeToValue<Any>(valueNode, Any::class.java)
    return Either.right(value) as Either.Right<Any>
  }
}

class JacksonArrowOptionSomeDeserializer:
  StdDeserializer<Some<Any>>(Some::class.java) {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): Some<Any> {
    val node: JsonNode = parser.codec.readTree(parser)
    val valueNode = node.get("t")
    val mapper: ObjectMapper = parser.codec as ObjectMapper
    val value = mapper.treeToValue<Any>(valueNode, Any::class.java)
    return Some(value)
  }
}

class JacksonArrowOptionNoneDeserializer:
  StdDeserializer<arrow.core.None>(arrow.core.None::class.java) {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): arrow.core.None {
    return arrow.core.None
  }
}
