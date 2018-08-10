package com.tartner.vertx.commands

import com.tartner.vertx.cqrs.*
import io.vertx.core.*
import io.vertx.core.eventbus.*

/** Represents the failure of a command due to some general issue, not an exception. */
interface GeneralCommandFailure: FailureReply {
  val message: String
}

/** Represents the failure of a command due to a exception. */
interface CommandFailureDueToException: FailureReply {
  val cause: Throwable
}

data class CommandFailedDueToException(override val cause: Throwable): CommandFailureDueToException
