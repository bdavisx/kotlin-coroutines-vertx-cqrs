package com.tartner.utilities.jackson

import arrow.core.*
import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.std.*

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
interface ArrowMixIn

class JacksonArrowEitherLeftDeserializer:
  StdDeserializer<Either.Left<Any, Any>>(Either.Left::class.java) {

  override fun deserialize(parser: JsonParser, context: DeserializationContext): Either.Left<Any, Any> {
    val node: JsonNode = parser.codec.readTree(parser)
    val valueNode = node.get("a")
    val mapper: ObjectMapper = parser.codec as ObjectMapper
    val value = mapper.treeToValue<Any>(valueNode, Any::class.java)
    return Either.left(value) as Either.Left<Any, Any>
  }

}

class JacksonArrowEitherRightDeserializer:
  StdDeserializer<Either.Right<Any,Any>>(Either::class.java) {

  override fun deserialize(parser: JsonParser, context: DeserializationContext): Either.Right<Any, Any> {
    val node: JsonNode = parser.codec.readTree(parser)
    val valueNode = node.get("b")
    val mapper: ObjectMapper = parser.codec as ObjectMapper
    val value = mapper.treeToValue<Any>(valueNode, Any::class.java)
    return Either.right(value) as Either.Right<Any, Any>
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
