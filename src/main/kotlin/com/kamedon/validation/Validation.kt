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
 *
 * Modified by bdavisx
 */
class Validation<T>(val validations: Map<String, ChildValidation<T>>) {

  companion object {
    operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
      val builder = ValidationBuilder<T>()
      return builder.apply(init).build()
    }
  }

  fun validate(value: T): ValidationIssues {
    val messages = mutableMapOf<String, List<String>>()
    validations.forEach { map ->
      val errors = map.value.validations
        .filter {!it.first.invoke(value)}
        .map { it.second }
        .takeIf { it.isNotEmpty() }

      errors?.also {
        messages[map.key] = it
      }
    }
    return ValidationIssues(messages)
  }
  }

class ValidationIssues(private val validations: Map<String, List<String>>) {
  val hasIssues: Boolean; get() = validations.isNotEmpty()

  /** Returns a single string representing the issues. */
  fun formatIssueMessages(): String {
    if (!hasIssues) return ""

    return validations
      .flatMap { it: Map.Entry<String, List<String>> -> it.value.map { it } }
      .fold(StringBuilder(), {builder, message -> builder.append(message).append("; ") })
      .toString()
  }
}

class ValidationBuilder<T> {
  private var childValidations: MutableMap<String, ChildValidation<T>> = mutableMapOf()

  operator fun String.invoke(init: ChildValidation<T>.() -> Unit) {
    childValidations[this] = ChildValidation<T>().apply(init)
  }

  fun build(): Validation<T> {
    return Validation(childValidations)
  }
}

class ChildValidation<T> {
  var validations: MutableList<Pair<T.() -> Boolean, String>> = mutableListOf()

  fun mustBe(validate: T.() -> Boolean) = validate

  infix fun (T.() -> Boolean).ifNot(error: String) {
    validations.add(this to error)
  }
}

