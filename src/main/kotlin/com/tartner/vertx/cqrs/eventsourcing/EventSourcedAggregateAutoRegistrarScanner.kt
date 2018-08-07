package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.commands.*
import com.tartner.vertx.cqrs.*
import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.kotlin.coroutines.*
import org.kodein.di.*
import org.kodein.di.generic.*
import org.reflections.*
import kotlin.reflect.*
import kotlin.reflect.full.*

/*
A higher level class EventSourcedAggregateStartup than the EventSourcingDelegate should search thru
all of the classes looking for @EventSourcedDelegate's and then register their @CreationHandler and
@SnapshotHandler annotations with whatever it was we created the other day
(RegisterInstantiationClassesForAggregateLocalCommand). The startup class will need to create the
factory needed to create aggregate.

Delegate should examine the class and find functions annotated with @CreationHandler,
@CommandHandler, @EventHandler, @SnapshotHandler. These methods will get called appropriately
when the Message<class they handle> comes thru the right "channel".
 */
class EventSourcedAggregateAutoRegistrarScanner(
  val kodein: Kodein,
  val commandSender: CommandSender
) {
  suspend fun scanAndRegisterEventSourcedAggregates(vertx: Vertx, eventBus: EventBus,
    vararg exampleClasses: KClass<*>) {

    val javaExampleClasses = exampleClasses.map{it.java}.toTypedArray()
    val reflections = Reflections(*javaExampleClasses)

    val aggregateClasses = reflections.getTypesAnnotatedWith(
      EventSourcedAggregate::class.java)

    @Suppress("UNCHECKED_CAST")
    aggregateClasses.forEach { javaClass: Class<*> ->
      val creationHandlers =
        javaClass.kotlin.members.filter { it.findAnnotation<CreationHandler>() != null }
      val creationParameterClasses = creationHandlers.map{ it: KCallable<*> ->
        val parameter: KParameter = it.parameters.first()
        parameter.type.classifier as KClass<AggregateEvent>
      }


      val snapshotHandlers =
        javaClass.kotlin.members.filter { it.findAnnotation<SnapshotHandler>() != null }
      val snapshotParameterClasses = snapshotHandlers.map{ it: KCallable<*> ->
        val parameter: KParameter = it.parameters.first()
        parameter.type.classifier as KClass<AggregateSnapshot>
      }

      val factory = kodein.direct.factory<AggregateId, CoroutineVerticle>(
        TT(javaClass))
      val command = RegisterInstantiationClassesForAggregateLocalCommand(
        factory, creationParameterClasses, snapshotParameterClasses)
      commandSender.send(eventBus, command)
    }
  }
}
