---
layout: post
title: How to Handle Command Registration
---

Each verticle that needs to handle commands will register for that command on the bus. Multiple deployments can register for the same command and will be routed in round-robin.

I've created an interface that the companion object can implement to determine how many instances to deploy based on the number of EventBus threads.
