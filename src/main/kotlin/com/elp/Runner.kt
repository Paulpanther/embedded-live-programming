package com.elp

import com.intellij.openapi.Disposable
import com.intellij.psi.PsiFile
import java.io.File
import java.util.concurrent.TimeUnit

class Frame(private val path: String): Thread() {
    private var running = true

    override fun run() {
        execute(path)
    }

    fun stopRunning() {
        running = false
        join()
    }

    private external fun execute(path: String)

    @Suppress("unused")
    private fun onIteration(probes: Array<Probe>): Boolean {
        val realProbes = probeService.probes
        val probesByCode = probes.groupBy { it.code }

        for (realProbe in realProbes) {
            val executedProbes = probesByCode[realProbe.code]
            val text = executedProbes?.joinToString(", ") { it.value } ?: "unreached"
            realProbe.updateText(text)
        }
        return running
    }
}

class Runner: Disposable {
    private var i = 0
    private val path = "/home/paul/dev/uni/embedded-live-programming-runner"
    private var frame: Frame? = null

    fun start(): Runner {
        System.load("$path/runner.so")
        return this
    }

    fun executeFile(file: PsiFile) {
        val content = file.text
        File("$path/tmp/code.cpp").writeText(content)
        val lib = "code${i++}"
        val cmd = "g++ -I $path/user_include -shared -fPIC $path/tmp/code.cpp -o $path/tmp/$lib.so"
        Runtime
            .getRuntime()
            .exec(cmd)
            .waitFor(20, TimeUnit.SECONDS)

        frame?.stopRunning()
        frame = Frame("$path/tmp/$lib.so").also { it.start() }
    }

    override fun dispose() {
        frame?.stopRunning()
    }
}
