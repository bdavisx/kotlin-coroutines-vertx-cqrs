/*
MIT License

Copyright (c) 2017 kamedon

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.kamedon.validation

/**
 * Created by Kamedon
 */
class Validation<T>(val validations: Map<String, ChildValidation<T>>) {

  companion object {
    operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
      val builder = ValidationBuilder<T>()
      return builder.apply(init).build()
    }
  }

  fun validate(value: T): Map<String, List<String>> {
    val messages = mutableMapOf<String, List<String>>()
    validations.forEach { map ->
      val errors = map.value.validations.filter { !it.first.invoke(value) }.map { it.second }.takeIf { it.isNotEmpty() }
      errors?.also {
        messages.put(map.key, it)
      }
    }
    return messages
  }

}

class ValidationBuilder<T> {
  var childValidations: MutableMap<String, ChildValidation<T>> = mutableMapOf()

  operator fun String.invoke(init: ChildValidation<T>.() -> Unit) {
    childValidations.put(this, ChildValidation<T>().apply(init))
  }

  fun build(): Validation<T> {
    return Validation(childValidations)
  }

}

class ChildValidation<T> {
  var validations: MutableList<Pair<T.() -> Boolean, String>> = mutableListOf()

  fun be(validate: T.() -> Boolean) = validate

  infix fun (T.() -> Boolean).not(error: String) {
    validations.add(this to error)
  }

}
