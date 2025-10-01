package com.github.tanokun.addon

import java.io.File
import java.net.URL
import java.util.*
import java.util.jar.JarFile

/**
 * 与えた delegate を使いつつ、指定の JarFile があれば
 * リソース探索で Jar 内のエントリを合成して返すラッパー ClassLoader。
 */
class DelegatingResourceClassLoader(
    private val delegate: ClassLoader,
    private val pluginJar: JarFile?
) : ClassLoader(delegate) {

    private fun jarFileUrl(): URL? {
        val j = pluginJar ?: return null
        return try {
            File(j.name).toURI().toURL()
        } catch (e: Throwable) {
            null
        }
    }

    override fun getResource(name: String): URL? {
        // まず delegate に任せる
        delegate.getResource(name)?.let { return it }

        // Jar がある場合は合成 URL を返す（存在確認してから）
        val jar = pluginJar ?: return null
        val path = if (name.startsWith("/")) name.substring(1) else name
        val entry = jar.getJarEntry(path) ?: jar.getJarEntry("$path/")
        if (entry != null) {
            val fileUrl = jarFileUrl() ?: return null
            // jar URL を生成 (例: jar:file:/path/to.jar!/com/github/)
            return try {
                URL("jar:${fileUrl.toExternalForm()}!/$path")
            } catch (e: Throwable) {
                null
            }
        }
        return null
    }

    override fun getResources(name: String): Enumeration<URL> {
        val delegateEnum = delegate.getResources(name)
        val urls = mutableListOf<URL>()
        while (delegateEnum.hasMoreElements()) urls.add(delegateEnum.nextElement())

        // delegate が空なら Jar の合成 URL を返す
        if (urls.isEmpty()) {
            val jar = pluginJar
            if (jar != null) {
                val path = if (name.startsWith("/")) name.substring(1) else name
                // パッケージ検索ではディレクトリエントリがなくても prefix にマッチするか確認
                val hasAny = jar.entries().asSequence().any { it.name.startsWith(path) }
                if (hasAny) {
                    jarFileUrl()?.let { fileUrl ->
                        try {
                            urls.add(URL("jar:${fileUrl.toExternalForm()}!/$path/"))
                        } catch (_: Throwable) { }
                    }
                }
            }
        }

        return Collections.enumeration(urls)
    }

    override fun getResourceAsStream(name: String) =
        delegate.getResourceAsStream(name) ?: run {
            val jar = pluginJar ?: return null
            val path = if (name.startsWith("/")) name.substring(1) else name
            val entry = jar.getJarEntry(path) ?: jar.getJarEntry("$path/")
            entry?.let { jar.getInputStream(it) }
        }
}