package com.elp.util

object NamingHelper {
    fun nextName(suggestion: String, names: List<String>): String {
        var i = names.size + 1
        var next: String
        do {
            next = suggestion + i++
        } while (next in names)
        return next
    }
}
