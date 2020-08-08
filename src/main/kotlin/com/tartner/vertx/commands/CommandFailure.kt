package com.tartner.vertx.commands

import com.tartner.vertx.cqrs.CorrelationId
import com.tartner.vertx.cqrs.FailureReply
import com.tartner.vertx.cqrs.HasCorrelationId
import io.vertx.core.AsyncResult
import io.vertx.core.eventbus.Message

/** Represents the failure of a command due to some general issue, not an exception. */
interface GeneralCommandFailure: FailureReply {
  val message: String
}

/** Represents the failure of a command due to a exception. */
interface CommandFailureDueToException: FailureReply, HasCorrelationId {
  val cause: Throwable
}

data class CommandFailedDueToException(override val cause: Throwable,
  override val correlationId: CorrelationId): CommandFailureDueToException

/**
This method will send a message reply in the result of a failure.

@param message The original eventBus message that started the request so a reply can be sent
@param result An AsyncResult that caused the failure
@param exceptionFailureFactory A factory that will create a reply message in the case that an
exception was the cause of the failure.
@param nonExceptionFailureFactory A factory that will create a reply message in the case that there
was no exception that was the cause of the failure (e.g. result.cause() == null).
 */
fun <T, R> replyForFailure(message: Message<T>, result: AsyncResult<R>,
  exceptionFailureFactory: (T) -> CommandFailureDueToException,
  nonExceptionFailureFactory: (T) -> GeneralCommandFailure) {

  val body = message.body()
  when (result.cause()) {
    null -> message.reply(nonExceptionFailureFactory(body))
    else -> message.reply(exceptionFailureFactory(body))
  }
}


