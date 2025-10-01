package com.github.tanokun.reactivesk.skriptadapter.common.dynamic

import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.Identifier
import java.io.File

interface DynamicClassDefinitionLoader {
    fun loadAllClassesFrom(folder: File)

    fun getAllDefinitions(): List<ClassDefinition>

    fun getClassDefinition(className: Identifier): ClassDefinition?
}