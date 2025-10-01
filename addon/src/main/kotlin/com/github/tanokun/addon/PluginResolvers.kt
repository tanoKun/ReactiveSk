package com.github.tanokun.addon

import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.pool.TypePool
import java.io.File
import java.util.jar.JarFile

class PluginResolvers private constructor(
    val locator: ClassFileLocator,
    val typePool: TypePool
) {
    companion object {
        fun fromClass(clazz: Class<*>): PluginResolvers {
            val origLoader = clazz.classLoader ?: ClassLoader.getSystemClassLoader()
            var jar: JarFile? = null
            try {
                val loc = clazz.protectionDomain?.codeSource?.location
                if (loc != null) {
                    val file = File(loc.toURI())
                    if (file.isFile && file.name.endsWith(".jar")) {
                        jar = JarFile(file)
                    }
                }
            } catch (_: Throwable) { }

            // ラッパー ClassLoader を作成して getResource 系を補完する
            val effectiveLoader = if (jar != null) {
                DelegatingResourceClassLoader(origLoader, jar)
            } else {
                origLoader
            }

            val locator = PluginClassFileLocator(effectiveLoader, jar)
            val pool = TypePool.Default.of(effectiveLoader)
            return PluginResolvers(locator, pool)
        }
    }

    fun close() {
        try {
            // 必要なら JarFile を閉じる
            if (locator is PluginClassFileLocator) {
                // Locator 側で管理している JarFile を閉じる実装があれば呼ぶ
            }
        } catch (_: Throwable) {}
    }
}