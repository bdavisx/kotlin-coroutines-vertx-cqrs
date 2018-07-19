---
layout: post
title: Vertx Serialization
---

While `json` is the [lingua franca](http://vertx.io/docs/vertx-core/kotlin/#_types_of_messages) of Vert.x, dealing with it by manually serializing and deserializing messages leaves a lot to be desired.

So we can create something that will serialize the commands and events for us. To integrate the serializer with Vertx, we need to create a [custom MessageCodec](http://vertx.io/docs/apidocs/io/vertx/core/eventbus/MessageCodec.html). Once the codec is registered for a class, when an object of the registered class is sent through the EventBus, it will be automatically serialized/deserialized as necessary.

The custom codec is implemented by the `EventBusJacksonJsonCodec` class. The class uses a correctly configured `ObjectMapper` to handle serialization. The `TypedObjectMapper` class has a companion object with a `default` property that will work for our purposes. Take a look at the source code for it for details on configuration.

>Due to how the Vert.x `EventBus` works, we have to register a codec for each class we want to send through. I haven't found any way to register for multiple classes with the same codec.

`EventBusJacksonJsonCodec` takes the `ObjectMapper` and the KClass that it will be serializing. It will not serialize any objects that are only being used locally, it will pass them through unchanged.

There are a couple of top level helper functions to help register the classes with Vert.x. They are both named `register` and can be found in the `EventBusCodecRegistration.kt` file. They are shown below:

By making the type `reified`, we can use the class metadata as well when needed.

```kotlin
/**
Registers `class T` with the vertx bus to automatically serialize to json using the specified mapper.
The handler will be registered as a consumer using `T.class.qualifiedName`.
 */
inline fun <reified T: Any> register(mapper: ObjectMapper, vertx: Vertx, handler: Handler<Message<T>>) {
    register<T>(mapper, vertx)

    vertx.eventBus().consumer<T>(T::class.qualifiedName,
        { message -> handler.handle(message) })
}

/**
Registers `class T` with the vertx bus to automatically serialize to json using the specified mapper.
 */
inline fun <reified T: Any> register(mapper: ObjectMapper, vertx: Vertx) {
    vertx.eventBus().registerDefaultCodec(T::class.java,
        EventBusJacksonJsonCodec(mapper, T::class))
}
```

