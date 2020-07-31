package com.tartner.vertx.kodein

import org.kodein.di.DKodeinAware
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic

/** Mirror of DKodeinAware.instance(tag: Any? = null). */
inline fun <reified T : Any> DKodeinAware.i(tag: Any? = null) = dkodein.Instance<T>(generic(), tag)

inline fun <reified T : Any> Kodein.i(tag: Any? = null) = direct.Instance<T>(generic(), tag)

