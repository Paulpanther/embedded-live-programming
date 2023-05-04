package com.elp

import com.intellij.openapi.Disposable
import com.intellij.psi.PsiFile
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.scheduleAtFixedRate

class Frame(
    private val path: String,
    private val mock: Boolean = false
): Thread() {
    private var running = true

    override fun run() {
        if (mock) {
            mockExecute()
        } else {
            execute(path)
        }
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
            val text = executedProbes?.joinToString(", ") { it.value } ?: continue
            realProbe.updateText(text)
        }
        return running
    }

    private fun mockExecute() {
        var i = 0

        Timer().scheduleAtFixedRate(0L, 10L) {
            for (probe in probeService.probes) {
                probe.updateText(i.toString())
            }
            i = (i + 1) % 256

            if (!running) {
                cancel()
            }
        }
    }
}

class Runner(
    private val mock: Boolean = false
): Disposable {
    private var i = 0
    private val runnerPath = "/home/paul/dev/uni/embedded-live-programming-runner"
    private val userCodePath = "/home/paul/dev/uni/embedded-live-programming-user-code"
    private var frame: Frame? = null

    fun start(): Runner {
        if (!mock) {
            System.load("$runnerPath/runner.so")
        }
        return this
    }

    fun executeFile(file: PsiFile) {
        if (mock) {
            if (frame == null) {
                frame = Frame("", true).also { it.start() }
            }
            return
        }

        val content = file.text
        File("$userCodePath/src/code.cpp").writeText(content)
        val lib = "code${i++}"
        val cmd = "$userCodePath/build.sh $lib"
        Runtime
            .getRuntime()
            .exec(cmd)
            .waitFor(20, TimeUnit.SECONDS)

        frame?.stopRunning()
        frame = Frame("$userCodePath/build/lib$lib.so").also { it.start() }
    }

    override fun dispose() {
        frame?.stopRunning()
    }
}
