package com.tartner.vertx

import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

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
