package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.commands.CommandSender
import com.tartner.vertx.cqrs.AggregateEvent
import com.tartner.vertx.cqrs.AggregateId
import com.tartner.vertx.cqrs.AggregateSnapshot
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.kodein.di.Kodein
import org.kodein.di.TT
import org.kodein.di.direct
import org.kodein.di.generic.factory
import org.reflections.Reflections
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

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

      val factory = kodein.direct.factory<AggregateId, CoroutineVerticle>(TT(javaClass))
      val command = RegisterInstantiationClassesForAggregateLocalCommand(
        factory, creationParameterClasses, snapshotParameterClasses)
      commandSender.send(eventBus, command)
    }
  }
}
