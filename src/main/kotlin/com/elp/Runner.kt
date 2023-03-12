package com.elp

class Runner {
    private val path = "/home/paul/dev/uni/embedded-live-programming-runner"

    fun start() {
        System.load("$path/runner.so")
    }

    external fun execute(path: String)

    fun onIteration(probes: Array<Probe>) {

    }
}
