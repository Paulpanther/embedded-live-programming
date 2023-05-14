package com.elp.util

open class Listeners<T> {
    private val listeners = mutableSetOf<T>()

    fun register(listener: T) {
        listeners += listener
    }

    fun call(operation: (listener: T) -> Unit) {
        listeners.forEach { operation(it) }
    }
}

class UpdateListeners: Listeners<() -> Unit>() {
    fun call() {
        call { it() }
    }
}
