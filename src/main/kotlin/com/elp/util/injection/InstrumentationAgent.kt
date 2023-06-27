package com.elp.util.injection

import com.sun.tools.attach.VirtualMachine
import java.lang.instrument.Instrumentation

class InstrumentationAgent {
    companion object {
        @JvmStatic
        fun agentmain(agentArgs: String, inst: Instrumentation) {
            error("HELLOHELLO")
        }

        fun load() {
        }
    }
}
