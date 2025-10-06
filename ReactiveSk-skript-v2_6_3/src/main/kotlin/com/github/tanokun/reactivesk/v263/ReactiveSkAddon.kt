package com.github.tanokun.reactivesk.v263

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.config.Node
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.lang.TriggerSection
import ch.njol.skript.registrations.Classes
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.tanokun.reactivesk.compiler.backend.codegen.JvmBytecodeGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.constructor.ConstructorGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.field.FieldsGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.method.MethodsGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.method.TriggerMetadata
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableResolver
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.SkriptAstBuilder
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.DynamicClassManager
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.ModuleClassResolver
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.classloader.DynamicClassDefinitionLoader
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.classloader.DynamicClassLoader
import com.github.tanokun.reactivesk.v263.attach.AttachWalkTriggerTransformer
import com.github.tanokun.reactivesk.v263.attach.caller.method.MethodCallerBytecodeGenerator
import com.github.tanokun.reactivesk.v263.caller.method.ConstructorCaller
import com.github.tanokun.reactivesk.v263.caller.method.MethodCaller
import com.github.tanokun.reactivesk.v263.intrinsics.JvmSetterIntrinsicsV263
import com.github.tanokun.reactivesk.v263.intrinsics.TriggerItemIntrinsicsV263
import com.github.tanokun.reactivesk.v263.intrinsics.VariableFramesIntrinsicsV263
import com.github.tanokun.reactivesk.v263.skript.DynamicClass
import com.github.tanokun.reactivesk.v263.skript.SkriptNodeAdapterV263
import com.github.tanokun.reactivesk.v263.skript.analyze.ast.*
import com.github.tanokun.reactivesk.v263.skript.resolve.clazz.ConstructorInjectorSection
import com.github.tanokun.reactivesk.v263.skript.resolve.clazz.FieldDefinitionMakerSection
import com.github.tanokun.reactivesk.v263.skript.resolve.clazz.FunctionDefinitionInjectorSection
import com.github.tanokun.reactivesk.v263.skript.resolve.clazz.SetCurrentResolvingClassSkriptEvent
import com.github.tanokun.reactivesk.v263.skript.resolve.variable.LocalTypedVariableDeclarationEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.array.TransformSingleTypeArrayExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.function.FunctionReturnEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.function.call.NonSuspendCallFunctionEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.function.call.NonSuspendCallFunctionExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.instantiation.InstantiationExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.instantiation.ResolveFieldValueEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.observe.ObserverSkriptEvent
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.CastExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.field.GetTypedValueFieldExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.field.SetTypedValueFieldEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.local.GetLocalTypedVariableExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.local.SetLocalTypedVariableEffect
import com.github.tanokun.reactivesk.v263.skript.serializer.DynamicInstanceSerializer
import com.github.tanokun.reactivesk.v263.skript.util.ReflectionClassesBySkript.getClassBySkript
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.logging.Logger

class ReactiveSkAddon : JavaPlugin() {
    companion object {
        lateinit var plugin: ReactiveSkAddon private set

        val classResolver = ModuleClassResolver(
            getLoadedClass = { dynamicManager.getLoadedClass(it) },
            staticClassResolver = { getClassBySkript(it.identifier) }
        )

        val bytecodeGenerator = JvmBytecodeGenerator(
            superClass = DynamicClass::class.java,
            constructorGenerator = ConstructorGenerator(
                classResolver,
                VariableFramesIntrinsicsV263::class.java,
                TriggerItemIntrinsicsV263::class.java,
            ),
            methodsGenerator = MethodsGenerator(
                classResolver,
                VariableFramesIntrinsicsV263::class.java,
                TriggerItemIntrinsicsV263::class.java,
            ),
            fieldsGenerator = FieldsGenerator(
                classResolver,
                JvmSetterIntrinsicsV263::class.java
            )
        )

        val dynamicManager: DynamicClassManager<DynamicClass> by lazy {
            DynamicClassManager(
                scriptRootFolder = Skript.getInstance().dataFolder,
                jvmBytecodeGenerator = bytecodeGenerator
            )
        }

        val definitionLoader: DynamicClassDefinitionLoader by lazy { dynamicManager.definitionLoader }

        val skriptAstBuilder: SkriptAstBuilder<SectionNode, TriggerItem> = SkriptAstBuilder(
            parsers = listOf(
                FunReturnLineParser,
                ResolveFieldVariableLineParser,
                ConditionalParser,
                LoopParser,
                OtherSectionParser
            ),
            skriptNodeAdapter = SkriptNodeAdapterV263
        )

        val typedVariableResolver = TypedVariableResolver<Node, TriggerSection>()

        val methodCallers = HashMap<Method, MethodCaller>()

        val constructorCallers = HashMap<Constructor<*>, ConstructorCaller>()

        val coroutineScope by lazy {
            val handler = CoroutineExceptionHandler { _, throwable ->
                throwable.printStackTrace()
            }
            CoroutineScope(plugin.minecraftDispatcher + SupervisorJob() + handler)
        }

        fun registerClass(dataFolder: File) {
            dataFolder.resolve("generated-classes").let { file ->
                if (!file.exists()) file.mkdirs()

                dynamicManager.definitionLoader.getAllDefinitions().forEach { definition ->
                    bytecodeGenerator.generateClass(definition).saveIn(file)

                    val loadedClass = dynamicManager.getLoadedClass(definition.className)
                        ?: throw IllegalStateException("Class '${definition.className.identifier}' is not loaded.")

                    val methodCallers = loadedClass.methods
                        .filter { method -> method.isAnnotationPresent(TriggerMetadata::class.java) }
                        .map { method -> MethodCallerBytecodeGenerator.generateClass(method) to method }

                    val constructorCallers = loadedClass.constructors
                        .filter { constructor -> constructor.parameters.size >= 1 }
                        .map { constructor -> MethodCallerBytecodeGenerator.generateClass(constructor) to constructor }

                    val callerClassLoader = DynamicClassLoader(
                        loadedClass.classLoader,
                        methodCallers.map { it.first } + constructorCallers.map { it.first }
                    )

                    methodCallers.forEach { (caller, method) ->
                        Companion.methodCallers[method] = callerClassLoader.loadClass(caller.typeDescription.name).newInstance() as MethodCaller
                    }

                    constructorCallers.forEach { (caller, constructor) ->
                        Companion.constructorCallers[constructor] = callerClassLoader.loadClass(caller.typeDescription.name).newInstance() as ConstructorCaller
                    }
                }
            }
        }

        fun registerClassToSkript(logger: Logger) {
            dynamicManager.definitionLoader.getAllDefinitions().forEach { definition ->
                try {
                    val loadedClass = dynamicManager.getLoadedClass(definition.className)
                        ?: throw IllegalStateException("Class '${definition.className.identifier}' is not loaded.")

                    Classes.registerClass(
                        ClassInfo(loadedClass, definition.className.identifier.lowercase())
                            .name(definition.className.identifier)
                            .user(definition.className.identifier, definition.className.identifier.lowercase())
                            .serializer(DynamicInstanceSerializer())
                    )

                } catch (e: Throwable) {
                    logger.warning("Failed to load class '${definition.className.identifier}' -> ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    lateinit var addon: SkriptAddon
        private set

    init {
        AttachWalkTriggerTransformer.install()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onEnable() {
        plugin = this

        ClassesRegister.registerAll()

        dynamicManager.reload().forEach {
            logger.info("Loaded class: $it")
        }

        registerClass(dataFolder)
        registerClassToSkript(logger)

        addon = Skript.registerAddon(this)
        registerToSkript()

        logger.info("ReactiveSk Addon has been enabled successfully!")
    }

    private fun registerToSkript() {
        // Class
        ConstructorInjectorSection.register()
        SetCurrentResolvingClassSkriptEvent.register()
        FieldDefinitionMakerSection.register()
        FunctionDefinitionInjectorSection.register()

        // Function
        FunctionReturnEffect.register()
        NonSuspendCallFunctionEffect.register()
        NonSuspendCallFunctionExpression.register()

        // Variable
        LocalTypedVariableDeclarationEffect.register()
        GetLocalTypedVariableExpression.register()
        SetLocalTypedVariableEffect.register()

        // Field
        GetTypedValueFieldExpression.register()
        SetTypedValueFieldEffect.register()

        // Instance
        InstantiationExpression.register()
        ResolveFieldValueEffect.register()

        // Observe
        ObserverSkriptEvent.register()

        // Util
        CastExpression.register()
        TransformSingleTypeArrayExpression.register()

    }

    override fun onDisable() {
        logger.info("ReactiveSk Addon has been disabled.")
    }
}
