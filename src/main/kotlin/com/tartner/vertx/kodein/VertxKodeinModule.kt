package com.tartner.vertx.kodein

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.file.FileSystem
import io.vertx.core.shareddata.SharedData
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

fun vertxKodeinModule(vertx: Vertx) = Kodein.Module("vert.x kodein") {
  bind<Vertx>() with singleton { vertx }
  bind<EventBus>() with singleton { vertx.eventBus() }
  bind<FileSystem>() with singleton { vertx.fileSystem() }
  bind<SharedData>() with singleton { vertx.sharedData() }
}

