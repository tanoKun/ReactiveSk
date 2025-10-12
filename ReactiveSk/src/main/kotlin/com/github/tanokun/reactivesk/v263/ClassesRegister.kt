package com.github.tanokun.reactivesk.v263

import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.classes.Parser
import ch.njol.skript.lang.ParseContext
import ch.njol.skript.registrations.Classes
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.skript.DynamicClassInfo

val identifierPattern = "[_a-zA-Z][a-zA-Z0-9]*".toRegex()

object ClassesRegister {
    fun registerAll() {
        identifier()
        dynamicClassInfo()
        void()
        arrayList()
    }

    fun identifier() {
        Classes.registerClass(ClassInfo(Identifier::class.java, "identifier")
            .parser(object : Parser<Identifier>() {
                override fun parse(s: String, context: ParseContext): Identifier? {
                    if (s.matches(identifierPattern)) return Identifier(s)

                    return null
                }

                override fun toString(o: Identifier, flags: Int): String = o.toString()


                override fun toVariableNameString(o: Identifier): String = variableNamePattern

                val variableNamePattern: String
                    get() = "[_a-zA-Z][a-zA-Z0-9]*"
            })
        )
    }

    fun void(){
        Classes.registerClass(ClassInfo(Void.TYPE, "void")
            .user("void")
        )
    }

    fun arrayList() {
        Classes.registerClass(ClassInfo(ArrayList::class.java, "array")
            .user("array")
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun dynamicClassInfo() {
        Classes.registerClass(ClassInfo(DynamicClassInfo::class.java, "dynamicclassinfo")
            .parser(object : Parser<DynamicClassInfo>() {
                override fun parse(s: String, context: ParseContext): DynamicClassInfo? {
                    val className = Identifier(s)

                    val classDefinition = ReactiveSkAddon.dynamicManager.definitionLoader.getClassDefinition(className) ?: return null
                    val dynamicClass = ReactiveSkAddon.dynamicManager.getLoadedClass(className) ?: return null

                    return DynamicClassInfo(classDefinition, dynamicClass)
                }

                override fun toString(o: DynamicClassInfo, flags: Int): String = o.toString()

                override fun toVariableNameString(o: DynamicClassInfo): String = variableNamePattern

                val variableNamePattern: String
                    get() = "[_a-zA-Z][a-zA-Z0-9]*"
            })
        )
    }
}