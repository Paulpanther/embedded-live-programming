package com.elp.util.injection

import com.intellij.openapi.diagnostic.thisLogger
import com.jetbrains.cidr.lang.psi.impl.OCReferenceElementImpl
import javassist.ClassClassPath
import javassist.ClassPool
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class ReferenceCallInjector: ClassFileTransformer {
    private val targetClass = OCReferenceElementImpl::class.java

    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        if (classBeingRedefined != targetClass) return null

        try {
            val classes = ClassPool.getDefault()
            classes.insertClassPath(ClassClassPath(targetClass))

            val clazz = classes.get(targetClass.name)
            val method = clazz.getDeclaredMethod("getReferences")
            method.insertAfter("")

            return clazz.toBytecode().also { clazz.detach() }
        } catch (e: Exception) {
            thisLogger().error("Error transforming class", e)
            return null
        }
    }
}
