---
layout: post
title: CQRS Design
---

# Event Sourcing

## EventSourcedAggregateRepositoryVerticle

This class will need to track "local" aggregates and load them into memory if they are not already.

It will need to have some kind of cache of loaded aggregates and possibly do some kind of LRU on the cache.

It will need to know to route commands to another instance? Or should they be addressed correctly anyway? NO, commands will only have an aggregateId, the "route" will need to be determined here.

It seems like this class would be a bottleneck if we only have one instance, but how do we deal with the shared cache and any caching of where to send commands wrt the network/addresses?

How do you determine where an aggregate goes? If you want to do it by some kind of data, you need that data available somehow. There could be a way to look it up (the needed data), but then cache
the result somehow.

 *****

We need to use the LocalMap along with AsyncLocks to handle this among multiple instances of this verticle. You can get the object from the map, if it's null obtain the lock for the aggregates address (you can use a string for a "lock address" too). Then create the aggregate and store it in the map (actually I think we'll be storing the address in the map).

Ended up using an AsyncSharedMap instead of local to share the address across the cluster.

So when we get an AggregateInvalidatedEvent we'll need to lock the address again and remove it from the cache. Then when we get a message for the aggregate we'll know to reload it.


## CommandAndEventDelegate

This class REALLY needs a better name. This class may be split at some point, not sure. I want to let things go for a little bit and then refactor vs. going the wrong way now wrt splitting.

These were my initial thoughts, what is actually implemented may be slightly different.

StoreEvents which fires off a database command to actually store the events. This decouples the delegate, but it will wait for a response. The database handler verticle can be scaled based on the optimum # of database connections.

TakeSnapshot - something needs to decide when a snapshot takes place, and not the aggregate. somehow the delegate needs to ask the aggregate for a snapshot, a query would do it. The delegate would need the aggId, then it could tell the aggregate to take a snapshot of itself and call the StoreSnapshot method on here. Although something else besides the delegate could decide when it's time to snapshot

StoreSnapshot - see above

RestoreState - starting w/ the last snapshot thru the current event, will need a way to apply them to the aggregate; who asks for the restore? I'm not sure this functionality is part of this class or not, but it's certainly related.

# Database

# Type Specific JDBCClient Classes

**** Special Note: May need to use SQLClient here vs. JDBCClient (and code could be wrong too), check docs

If we're going to be using Dependency Injection, we can pass in a `JDBCClient` to any verticle that needs it'. The issue is that we need multiple connections, at the least we need one for the EventSourcing connection and one for the QueryModel. But if we declare a dependency on `JDBCClient`, then we can't be sure that we get the correct `JDBCClient`. We might want the Query client, but we accidently pass in the `EventSourcing` one instead. There's no way for the complier to detect this if we declare a dependency on `JDBCClient`. But if we create two interfaces that extend `JDBCClient`, for example `QueryJDBCClient` and `EventSourcingJDBCClient`, then we can specify which one we need in our verticle constructors and there's no chance to get the wrong one.

```kotlin
interface QueryJDBCClient: JDBCClient

class QueryJDBCClientImpl(actualClient: JDBCClient): JDBCClient by actualClient, QueryJDBCClient
```

Notice how the implementation of QueryJDBCClient simply uses a "real" `JDBCClient` to do the work. So it's very little code to add to your project, and you don't even need to test the code it's so simple. You should integration test the code that creates `actualClient`, but outside of that what would you actually test for the above code?

So then a class that needs the QueryJDBCClient could look like this:

```kotlin
class UsersQueryHandlerVerticle @Inject constructor(
    val jdbcClient: QueryJDBCClient, val commandBusAdapter: CommandBusAdapter)
    : AbstractVerticle() {}
```
