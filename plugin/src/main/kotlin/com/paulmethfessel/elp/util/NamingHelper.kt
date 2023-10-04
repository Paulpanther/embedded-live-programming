package com.paulmethfessel.elp.util

object NamingHelper {
    fun nextName(suggestion: String, names: List<String>, separator: String = ""): String {
        var i = names.size + 1
        var next: String
        do {
            next = "$suggestion$separator${i++}"
        } while (next in names)
        return next
    }
}
