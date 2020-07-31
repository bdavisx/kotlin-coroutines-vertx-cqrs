package com.tartner.vertx.functional

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right

fun <L: Any> L.createLeft() = Either.Left(this)
fun <R: Any> R.createRight() = Either.Right(this)

inline fun <R> resultOf(f: () -> R): Either<Throwable, R> = try {
  Right(f())
} catch (e: Throwable) {
  Left(e)
}

suspend inline fun <L, R, C> Either<L, R>.mapS(crossinline f: suspend (R) -> C)
  : Either<L, C> = foldS({Either.Left(it)}, {Either.Right(f(it))})

suspend inline fun <L, R, C> Either<L, R>.flatMapS(crossinline f: suspend (R) -> Either<L, C>)
  : Either<L, C> = foldS({ Left(it) }, { f(it) })

suspend inline fun <L, R, C> Either<L, R>.foldS(
  crossinline fl: suspend (L) -> C, crossinline fr: suspend (R) -> C): C = when (this) {
    is Either.Right<R> -> fr(b)
    is Either.Left<L> -> fl(a)
}
