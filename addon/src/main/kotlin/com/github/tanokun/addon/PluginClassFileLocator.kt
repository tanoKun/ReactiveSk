package com.github.tanokun.addon

import net.bytebuddy.dynamic.ClassFileLocator
import java.io.InputStream
import java.util.jar.JarFile

class PluginClassFileLocator(
    private val loader: ClassLoader,
    private val jarFile: JarFile? = null
) : ClassFileLocator {
    override fun locate(name: String): ClassFileLocator.Resolution {
        val path = name.replace('.', '/') + ".class"
        // 1) クラスローダー経由
        try {
            val stream: InputStream? = loader.getResourceAsStream(path)
            if (stream != null) {
                return ClassFileLocator.Resolution.Explicit(stream.readBytes())
            }
        } catch (_: Throwable) { /* ignore */ }

        // 2) JAR 内エントリを直接読む（jarFile が指定されている場合）
        try {
            val jar = jarFile
            if (jar != null) {
                val entry = jar.getJarEntry(path)
                if (entry != null) {
                    jar.getInputStream(entry).use { ins ->
                        return ClassFileLocator.Resolution.Explicit(ins.readBytes())
                    }
                }
            }
        } catch (_: Throwable) { /* ignore */ }

        return ClassFileLocator.Resolution.Illegal(name)
    }

    override fun close() {
        // JarFile のクローズは所有者が行う想定。ここでは noop。
    }
}