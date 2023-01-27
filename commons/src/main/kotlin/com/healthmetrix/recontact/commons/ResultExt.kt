package com.healthmetrix.recontact.commons

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

fun <V, E : Throwable> Result<V, E>.orThrow(): V = when (this) {
    is Ok -> value
    is Err -> throw error
}

fun <E> E.err() = Err(this)

fun <V> V.ok() = Ok(this)
