package com.github.tanokun.addon.clazz.loader

import SkriptClassDefinitionLexer
import SkriptClassDefinitionParser
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.registrations.Classes
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.serializer.InstanceSerializer
import jdk.jfr.internal.JVM
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.InvokeDynamic
import net.bytebuddy.implementation.bytecode.StackManipulation
import net.bytebuddy.jar.asm.MethodVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

object StaticClassDefinitionLoader {
    private val _loadedClasses = mutableSetOf<Class<out AnyInstance>>()

    val loadedClasses: Set<Class<out AnyInstance>> get() = _loadedClasses

    fun load(folder: File) {
        recursiveLoad(folder)
    }

    fun recursiveLoad(folder: File) {
        if (!folder.exists() || !folder.isDirectory) {
            throw IllegalArgumentException("Provided folder does not exist or is not a directory: ${folder.path}")
        }

        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                recursiveLoad(file)
                return@forEach
            }

            if (file.extension != "sk") return@forEach

            file.useLines { lines ->
                lines
                    .filter { line -> line.startsWith("class") }
                    .forEach { line -> parseClass(line) }
            }
        }
    }

    private fun parseClass(line: String) {
        val input = CharStreams.fromString(line)
        val lexer = SkriptClassDefinitionLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = SkriptClassDefinitionParser(tokens)

        parser.classDefinition().let { ctx ->
            val className = ctx.IDENTIFIER().text
            val dynamicClass = createDynamicClass(className)

            Classes.registerClass(
                ClassInfo(dynamicClass, className.lowercase())
                    .serializer(InstanceSerializer())
                    .name(className)
                    .user(className, className.lowercase())
            )

            _loadedClasses.add(dynamicClass)
        }
    }

    private fun createDynamicClass(className: String): Class<out AnyInstance> {
        val fqcn = "com.github.tanokun.addon.generated.$className"

        val dynamicClass = ByteBuddy(ClassFileVersion.JAVA_V8)
            .subclass(AnyInstance::class.java)
            .name(fqcn)
            .defineMethod("test", String::class.java, Visibility.PUBLIC)
            .intercept(object : Implementation.Simple(StackManipulation.Compound(
                object : StackManipulation {
                    override fun isValid(): Boolean = true

                    override fun apply(
                        methodVisitor: MethodVisitor,
                        implementationContext: Implementation.Context,
                    ): StackManipulation.Size {
                    }
                }

            )){})
            .make()
            .load(AnyInstance::class.java.classLoader, ClassLoadingStrategy.Default.INJECTION)
            .loaded

        println("Generated dynamic $dynamicClass")
        return dynamicClass

    }
}