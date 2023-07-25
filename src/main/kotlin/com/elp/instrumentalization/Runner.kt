package com.elp.instrumentalization

import com.elp.model.Probe
import com.elp.services.probeService
import com.elp.util.error
import com.elp.util.logTime
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiFile
import java.io.File
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.scheduleAtFixedRate

class Frame(
    private val path: String,
    private val mock: Boolean = false
): Thread() {
    private var running = true
    private var firstResult = true

    override fun run() {
        if (mock) {
            mockExecute()
        } else {
            try {
                logTime("Execute")
                execute(path)
            } catch(_: Exception) {
                error("Error in Cpp Runtime")
            }
        }
    }

    fun stopRunning() {
        running = false
        logTime("Start Join")
        join()
        logTime("End Join")
    }

    private external fun execute(path: String)

    @Suppress("unused")
    private fun onIteration(probes: Array<Probe>): Boolean {
//        probeService.stopLoading()
        if (firstResult) {
            firstResult = false
            logTime("First result")
        }

        val realProbes = probeService.probes.values.flatten()
        val probesByCode = probes.groupBy { it.code }

        for (realProbe in realProbes) {
            val executedProbes = probesByCode[realProbe.code] ?: listOf()
            val lastExecutedProbe = executedProbes.lastOrNull() ?: continue
            realProbe.update(lastExecutedProbe)
        }
        return running
    }

    private fun mockExecute() {
        var i = 0

        Timer().scheduleAtFixedRate(0L, 20L) {
            for (probe in probeService.probes.values.flatten()) {
                probe.update(Probe(probe.code, i.toString(), "int"))
            }
            i = (i + 10) % 4097

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
    private var lastLib: String? = null

    fun start(): Runner {
        if (!mock) {
            System.load("$runnerPath/runner.so")
        }
        return this
    }

    fun executeFiles(files: List<PsiFile>) {
        if (mock) {
            if (frame == null) {
                frame = Frame("", true).also { it.start() }
            }
            return
        }

        File("$userCodePath/src/user").listFiles()?.forEach { it.delete() }
        for (file in files) {
            File("$userCodePath/src/user/${file.name}").writeText(file.text)
        }

        logTime("After writing files")

        val lib = "code${i++}"
        lastLib = lib
        val cmd = "$userCodePath/build.sh $lib"
        Runtime
            .getRuntime()
            .exec(cmd)
            .waitFor(20, TimeUnit.SECONDS)
        logTime("After build")

        val libFile = File("$userCodePath/build/lib$lib.so")
        if (!libFile.exists()) {
            val project = files.firstOrNull()?.project
            project?.error("Could not build project. Please check for errors.")
            return
        }

        logTime("Starting frame")
        frame?.stopRunning()
        frame = Frame("$userCodePath/build/lib$lib.so").also { it.start() }
    }

    fun stop() {
        frame?.stopRunning();
    }

    fun restart() {
        val lib = lastLib ?: return
        frame?.stopRunning()
        frame = Frame("$userCodePath/build/lib$lib.so").also { it.start() }
    }

    override fun dispose() {
        frame?.stopRunning()
    }
}
