# Kotlin Coroutines & Vert.x CQRS Library

# Pausing the work

Right now I'm going to stop working on the Vert.x version of this library. Vert.x is great, but it seems like a ton of work compared to more traditional programming (at least the way I was approaching it), and I currently have an exception happening with very little to go on in terms of what caused it. There's none of my code in the stack trace except for a utility. I *could* track the problem down, but my main concern is that in production, it would be much, much harder. I switched to Kotlin to cut down on verbosity (not the only reason, but one), and this library just makes it worse. I realize that there's a huge benefit to writing the code this way, but still, it's incredibly verbose.

I'm going to take a look @ the Kotlin coroutines version again, but perhaps Spring reactive is another option if coroutines aren't fully baked yet (although I think they're leaving experimental status in 1.3?)

Much of the documentation still applies to any implementation.

---

This is totally pre-pre-alpha everything subject to change right now. I'm trying to figure out what works, then make it easy to use.

Some things in `docs/` might be completely wrong or out of date at this point, some things are just stream of consciousness and way overcomplicated and/or just wrong at this point. At some point there's going to be massive simplification.
