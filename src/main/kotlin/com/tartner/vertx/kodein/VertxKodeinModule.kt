package com.tartner.vertx.kodein

import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.core.file.*
import io.vertx.core.shareddata.*
import org.kodein.di.*
import org.kodein.di.generic.*

fun vertxKodeinModule(vertx: Vertx) = Kodein.Module("vert.x kodein") {
  bind<Vertx>() with singleton { vertx }
  bind<EventBus>() with singleton { vertx.eventBus() }
  bind<FileSystem>() with singleton { vertx.fileSystem() }
  bind<SharedData>() with singleton { vertx.sharedData() }
}

