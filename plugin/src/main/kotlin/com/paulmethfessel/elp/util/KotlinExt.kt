package com.paulmethfessel.elp.util

import com.intellij.openapi.util.Ref
import java.sql.Timestamp

/** same as i++ */
fun Ref<Int>.getAndPostInc(): Int = get().also { set(get() + 1) }

fun <K, V> MutableMap<K, MutableList<V>>.appendValue(key: K, value: V) {
    getOrPut(key) { mutableListOf() } += value
}

fun <K, V> MutableMap<K, MutableList<V>>.appendValue(key: K, value: List<V>) {
    getOrPut(key) { mutableListOf() } += value
}

private const val LOG_TIME_ENABLED = false
fun logTime(msg: String) {
    if (LOG_TIME_ENABLED) {
        println("$msg: ${Timestamp(System.currentTimeMillis())}")
    }
}
