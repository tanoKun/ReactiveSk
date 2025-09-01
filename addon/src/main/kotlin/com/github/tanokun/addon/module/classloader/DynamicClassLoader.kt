package com.github.tanokun.addon.module.classloader

import net.bytebuddy.dynamic.DynamicType

class DynamicClassLoader(
    parent: ClassLoader,
    unloadedTypes: List<DynamicType.Unloaded<*>>
) : ClassLoader(parent) {
    private val byteCodeByName: Map<String, ByteArray> = unloadedTypes.associateBy(
        { it.typeDescription.name },
        { it.bytes }
    )

    override fun findClass(name: String): Class<*> {
        val byteCode = byteCodeByName[name]
        if (byteCode != null) {
            return defineClass(name, byteCode, 0, byteCode.size)
        }
        return super.findClass(name)
    }
}