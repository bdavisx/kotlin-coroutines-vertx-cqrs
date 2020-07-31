package com.tartner.vertx.cqrs

import io.vertx.core.AbstractVerticle

/**
We'll need a startup module that the user will need to pass vertx to. It will take care of starting
the SagaManager on each startup.
 */
class SagaManager: AbstractVerticle()
