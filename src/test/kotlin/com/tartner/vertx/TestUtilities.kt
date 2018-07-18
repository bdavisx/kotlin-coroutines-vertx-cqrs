package com.tartner.vertx

import java.time.*

val Int.seconds get() = Duration.ofSeconds(this.toLong())
val Int.milliSeconds get() = Duration.ofMillis(this.toLong())

val FastActorResponseTime: Duration = 10.milliSeconds
