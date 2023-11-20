package com.paulmethfessel.elp.execution

import com.paulmethfessel.elp.model.Probe
import com.paulmethfessel.elp.services.probeService
import com.paulmethfessel.elp.util.error
import com.paulmethfessel.elp.util.logTime
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiFile
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.scheduleAtFixedRate

/**
 * Frame of execution. Will be restarted everytime changes happen.
 * execute calls C++ function through JNI.
 * onIteration is called by C++. If false is returned C++ program will terminate
 */
class Frame(
    private val path: String,
    private val mock: Boolean
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
        join(1000)
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

/**
 * Runner for C++ backend.
 * When files should be executed it will copy them into the user-code environment and build it.
 * The resulting dyn-lib will be sent to the C++ runner and included there.
 */
class Runner(
    private val mock: Boolean
): Disposable {
    private var i = 0
    private val runner = File(System.getenv("ELP_RUNNER_PATH"))
        .also { if (!it.exists()) error("Invalid directory for backend: ${it.absolutePath}") }
    private val userCode = File(System.getenv("ELP_USER_CODE_PATH"))
        .also { if (!it.exists()) error("Invalid directory for user-code: ${it.absolutePath}") }

    private var frame: Frame? = null
    private var lastLib: String? = null

    fun start(): Runner {
        if (!mock) {
            System.load(File(runner, "runner.so").absolutePath)
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

        File(userCode, "src/user").listFiles()?.forEach { it.delete() }
        for (file in files) {
            File(userCode, "src/user/${file.name}").writeText(file.text)
        }

        logTime("After writing files")

        val lib = "code${i++}"
        lastLib = lib
        Runtime
            .getRuntime()
            .exec(arrayOf(File(userCode, "build.sh").absolutePath, lib))
            .waitFor(20, TimeUnit.SECONDS)
        logTime("After build")

        // lib file name has to change else C++ will not load it
        val libFile = File(userCode, "build/lib$lib.so")
        if (!libFile.exists()) {
            val project = files.firstOrNull()?.project
            project?.error("Could not build project. Please check for errors.")
            return
        }

        logTime("Starting frame")
        frame?.stopRunning()
        frame = Frame(File(userCode, "build/lib$lib.so").absolutePath, mock).also { it.start() }
    }

    fun stop() {
        frame?.stopRunning();
    }

    fun restart() {
        val lib = lastLib ?: return
        frame?.stopRunning()
        frame = Frame(File(userCode, "build/lib$lib.so").absolutePath, mock).also { it.start() }
    }

    override fun dispose() {
        frame?.stopRunning()
    }
}
