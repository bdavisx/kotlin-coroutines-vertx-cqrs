package com.tartner.vertx.codecs

import org.nustaq.serialization.*
import java.time.*

class OffsetDateTimeSerializer: FSTBasicObjectSerializer() {
  override fun writeObject(out: FSTObjectOutput?, toWrite: Any?, clzInfo: FSTClazzInfo?,
    referencedBy: FSTClazzInfo.FSTFieldInfo?, streamPosition: Int) {

    val offsetDateTime = toWrite as OffsetDateTime
    out!!.writeObject(offsetDateTime.toLocalDateTime())
    out.writeObject(offsetDateTime.offset)
  }
}

//class LocalDateTimeSerializer: FSTBasicObjectSerializer() {
//  override fun writeObject(out: FSTObjectOutput?, toWrite: Any?, clzInfo: FSTClazzInfo?,
//    referencedBy: FSTClazzInfo.FSTFieldInfo?, streamPosition: Int) {
//
//    val localDateTime = toWrite as LocalDateTime
//    out!!.writeObject(localDateTime.toLocalDateTime())
//    out!!.writeObject(localDateTime.offset)
//  }
//}
