package com.tartner.vertx.cqrs.eventsourcing

import com.tartner.vertx.cqrs.*
import io.kotlintest.*
import org.junit.*
import org.reflections.*
import kotlin.reflect.*
import kotlin.reflect.full.*

class EventSourcedAggregateAutoRegistrarScannerTest {
  @Test
  fun something() {
    val aggregateClass = EventSourcedTestAggregate::class.java
    val reflections = Reflections(aggregateClass)

    val aggregateClasses: MutableSet<Class<*>> =
      reflections.getTypesAnnotatedWith(EventSourcedAggregate::class.java)

    aggregateClasses.contains(aggregateClass) shouldBe true

    @Suppress("UNCHECKED_CAST")
    aggregateClasses.forEach { javaClass: Class<*> ->
      val creationHandlers = javaClass.kotlin.members.filter { it.findAnnotation<CreationHandler>() != null }
      val creationParameterClasses = creationHandlers.map { it: KCallable<*> ->
        val parameter: KParameter = it.parameters[1]
        parameter.type.classifier as KClass<AggregateEvent>
      }

      val snapshotHandlers = javaClass.kotlin.members.filter { it.findAnnotation<SnapshotHandler>() != null }
      val snapshotParameterClasses = snapshotHandlers.map { it: KCallable<*> ->
        val parameter: KParameter = it.parameters[1]
        parameter.type.classifier as KClass<AggregateSnapshot>
      }

      println(creationParameterClasses)
      println(snapshotParameterClasses)
    }
  }
}
