package com.github.tanokun.addon.clazz.definition.parse

import ch.njol.skript.classes.ClassInfo
import com.github.tanokun.addon.clazz.ClassRegistry
import com.github.tanokun.addon.clazz.definition.ClassDefinition
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.definition.field.FieldDefinition
import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition
import com.github.tanokun.addon.instance.AnyInstance

var currentlyCombinator: ParsedClassContentsCombinator? = null

class ParsedClassContentsCombinator(
    private val registry: ClassRegistry, val className: Identifier, private val classInfo: ClassInfo<out AnyInstance>
) {
    private val functions: ArrayList<FunctionDefinition> = arrayListOf()

    var fields: List<FieldDefinition>? = null
        set(value) {
            field = value

            tryConstruct()
        }

    fun addFunction(definition: FunctionDefinition) {
        functions.add(definition)

        tryConstruct()
    }

    fun tryConstruct() {
        val fields = this.fields ?: return
        val definition = ClassDefinition(className, classInfo, fields, functions)

        registry.registerClass(className, definition)
    }
}