package com.elp.util

import com.intellij.openapi.util.Ref

/** same as i++ */
fun Ref<Int>.getAndPostInc(): Int = get().also { set(get() + 1) }

fun <K, V> MutableMap<K, MutableList<V>>.appendValue(key: K, value: V) {
    getOrPut(key) { mutableListOf() } += value
}

fun <K, V> MutableMap<K, MutableList<V>>.appendValue(key: K, value: List<V>) {
    getOrPut(key) { mutableListOf() } += value
}
