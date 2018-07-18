package com.tartner.vertx

import io.vertx.core.*
import io.vertx.core.logging.*
import io.vertx.ext.unit.*
import io.vertx.ext.unit.junit.*
import org.junit.*
import org.junit.runner.*

fun <T> TestContext.assertResultTrue(result: AsyncResult<T>) {
  if (result.failed()) {
    this.fail()
  }
}

@RunWith(VertxUnitRunner::class)
abstract class AbstractVertxTest {
  private val log = LoggerFactory.getLogger(this.javaClass)

  var vertx: Vertx = Vertx.vertx()

  @Before
  fun beforeEach(context: TestContext) {
    log.debugIf {"Running test for ${this::class.qualifiedName}"}

    vertx = Vertx.vertx()
    vertx.exceptionHandler(context.exceptionHandler())

  }

  @After
  fun afterEach(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }
}
