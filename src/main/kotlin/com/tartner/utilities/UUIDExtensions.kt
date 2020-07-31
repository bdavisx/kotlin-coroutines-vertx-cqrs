/**
 * Copyright (C) 2018 Bill W. Davis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tartner.utilities

import java.util.Arrays
import java.util.UUID

/**
 * Copied from
 * https://github.com/NessComputing/components-ness-core/blob/master/src/main/java/com/nesscomputing/uuid/NessUUID.java
 * and converted to Kotlin. I couldn't find the ness one in maven.
 *
 * A class that provides an alternate implementation of [UUID.fromString] and [UUID.toString].
 *
 * The version in the JDK uses [String.split] which does not compile the regular
 * expression that is used for splitting the UUID string and results in the allocation of multiple
 * strings in a string array. We decided to write [NessUUID] when we ran into performance issues
 * with the garbage produced by the JDK class.
 */


// lookup is an array indexed by the **char**, and it has
// valid values set with the decimal value of the hex char.
private val lookup = buildLookup()
private const val DASH = -1
private const val ERROR = -2
private fun buildLookup(): LongArray {
  val lu = LongArray(128)
  Arrays.fill(lu, ERROR.toLong())
  lu['0'.toInt()] = 0
  lu['1'.toInt()] = 1
  lu['2'.toInt()] = 2
  lu['3'.toInt()] = 3
  lu['4'.toInt()] = 4
  lu['5'.toInt()] = 5
  lu['6'.toInt()] = 6
  lu['7'.toInt()] = 7
  lu['8'.toInt()] = 8
  lu['9'.toInt()] = 9
  lu['a'.toInt()] = 10
  lu['b'.toInt()] = 11
  lu['c'.toInt()] = 12
  lu['d'.toInt()] = 13
  lu['e'.toInt()] = 14
  lu['f'.toInt()] = 15
  lu['A'.toInt()] = 10
  lu['B'.toInt()] = 11
  lu['C'.toInt()] = 12
  lu['D'.toInt()] = 13
  lu['E'.toInt()] = 14
  lu['F'.toInt()] = 15
  lu['-'.toInt()] = DASH.toLong()
  return lu
}

// FROM STRING

fun uuidFromString(str: String): UUID {
  val len = str.length
  if (len != 36) {
    throw IllegalArgumentException("Invalid UUID string (expected to be 36 characters long)")
  }
  val vals = LongArray(2)
  var shift = 60
  var index = 0
  for (i in 0 until len) {
    val c = str[i].toInt()
    if (c >= lookup.size || lookup[c] == ERROR.toLong()) {
      throw IllegalArgumentException(
        "Invalid UUID string (unexpected '" + str[i] + "' at position " + i + " -> " + str + " )")
    }

    if (lookup[c] == DASH.toLong()) {
      if ((i - 8) % 5 != 0) {
        throw IllegalArgumentException(
          "Invalid UUID string (unexpected '-' at position $i -> $str )")
      }
      continue
    }
    vals[index] = vals[index] or (lookup[c] shl shift)
    shift -= 4
    if (shift < 0) {
      shift = 60
      index++
    }
  }
  return UUID(vals[0], vals[1])
}

// TO STRING

// recode is 2-byte arrays representing the hex representation of every byte value (all 256)
private val recode = buildByteBlocks()

private fun buildByteBlocks(): Array<CharArray> =
  Array(256, { String.format("%02x", it).toCharArray() })

fun UUID.toStringFast(): String {
  var msb = this.mostSignificantBits
  var lsb = this.leastSignificantBits
  val uuidChars = CharArray(36)
  var cursor = uuidChars.size
  while (cursor > 24) {
    cursor -= 2
    System.arraycopy(recode[(lsb and 0xff).toInt()], 0, uuidChars, cursor, 2)
    lsb = lsb ushr 8
  }
  uuidChars[--cursor] = '-'
  while (cursor > 19) {
    cursor -= 2
    System.arraycopy(recode[(lsb and 0xff).toInt()], 0, uuidChars, cursor, 2)
    lsb = lsb ushr 8
  }
  uuidChars[--cursor] = '-'
  while (cursor > 14) {
    cursor -= 2
    System.arraycopy(recode[(msb and 0xff).toInt()], 0, uuidChars, cursor, 2)
    msb = msb ushr 8
  }
  uuidChars[--cursor] = '-'
  while (cursor > 9) {
    cursor -= 2
    System.arraycopy(recode[(msb and 0xff).toInt()], 0, uuidChars, cursor, 2)
    msb = msb ushr 8
  }
  uuidChars[--cursor] = '-'
  while (cursor > 0) {
    cursor -= 2
    System.arraycopy(recode[(msb and 0xff).toInt()], 0, uuidChars, cursor, 2)
    msb = msb ushr 8
  }
  return String(uuidChars)
}

/** Does not include dashes in output string. */
fun UUID.toStringFastClear(): String {
  val full = this.toStringFast()
//4d866a47-9a89-45b0-b6a0-484641e61698
//---------1---------2---------3---------4
//1234567890123456789012345678901234567890

  return StringBuilder()
    .append(full.substring(0,8))
    .append(full.substring(9,13))
    .append(full.substring(14,18))
    .append(full.substring(19,23))
    .append(full.substring(24))
    .toString()
}
