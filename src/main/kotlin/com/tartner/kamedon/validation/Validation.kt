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

package com.tartner.kamedon.validation

/**
 * Created by Kamedon
 *
 * Modified by bdavisx (changed `be` to `mustBe` and `not` to `IfNot`. So now code reads like this:
      "name" { mustBe { !name.isBlank() } ifNot "name: must not be blank"
 *
 * I (bdavisx) just liked how this read vs. the original; otherwise it's an awesome, compact way
 * to deal with validation.
 */
class Validation<T>(val validations: Map<String, ChildValidation<T>>) {

  companion object {
    operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
      val builder = ValidationBuilder<T>()
      return builder.apply(init).build()
    }
  }

  suspend fun validate(value: T): ValidationIssues? {
    val messages = mutableMapOf<String, List<String>>()
    validations.forEach { validation: Map.Entry<String, ChildValidation<T>> ->
      val errors = validation.value.validations
        .filter {!it.first.invoke(value)}
        .map { it.second }
        .takeIf { it.isNotEmpty() }

      errors?.also {it: List<String> ->
        messages[validation.key] = it
      }
    }

    return if (messages.isEmpty()) { null }
      else { ValidationIssues(messages) }
  }
}

/** There will *always* be validations if you get an instance of this class. */
class ValidationIssues(val validations: Map<String, List<String>>) {
  init {
    if (validations.isEmpty()) { throw IllegalArgumentException("Can't have empty validations") }
  }

  /** Returns a single string representing the issues. */
  fun formatIssueMessages(): String {
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
  var validations: MutableList<Pair<suspend T.() -> Boolean, String>> = mutableListOf()

  fun mustBe(validate: suspend T.() -> Boolean) = validate

  infix fun (suspend T.() -> Boolean).ifNot(error: String) { validations.add(this to error) }
}

