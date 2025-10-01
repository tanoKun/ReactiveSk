package com.github.tanokun.reactivesk.compiler.backend.codegen

import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.compiler.backend.codegen.method.MethodsGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFunctionNameOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFunctionTriggerField
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.TriggerItemIntrinsics
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.VariableFramesIntrinsics
import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.Identifier
import io.mockk.every
import io.mockk.mockk
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.dynamic.scaffold.TypeValidation
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import kotlin.test.Test

class MethodsGeneratorTest {
    @Test
    fun `defineAllMethods actually generates method and trigger field`() {
        val classResolver = mockk<ClassResolver> {
            every { resolveTypeDescription(Identifier("String")) } returns TypeDescription.ForLoadedType.of(String::class.java)
            every { resolveTypeDescription(Identifier("String"), false) } returns TypeDescription.ForLoadedType.of(String::class.java)
        }

        val func = mockk<ClassDefinition.Function>()
        every { func.functionName } returns Identifier("doSomething")
        every { func.modifiers } returns 0
        every { func.returns } returns ClassDefinition.Function.Returns(Identifier("String"), false)
        every { func.parameters } returns emptyList()
        every { func.throws } returns emptyList()

        val classDefinition = mockk<ClassDefinition>()
        every { classDefinition.functions } returns listOf(func)

        val generator = MethodsGenerator(
            classResolver,
            variableFramesIntrinsics = TestHelpers::class.java,
            triggerItemIntrinsics = TestHelpers2::class.java,
            isImplementingBeginFrame = true
        )

        val builder = ByteBuddy(ClassFileVersion.JAVA_V8)
            .with(TypeValidation.DISABLED)
            .subclass(Any::class.java)
            .name("generated.TestGeneratedMethods")

        val resultBuilder = generator.defineAllMethods(builder, classDefinition)

        val dynamicType = resultBuilder.make()
        val loaded = dynamicType
            .load(this::class.java.classLoader, ClassLoadingStrategy.Default.WRAPPER)
            .apply { saveIn(File("generated-classes")) }
            .loaded

        val methodName = internalFunctionNameOf("doSomething")
        val fieldName = internalFunctionTriggerField("doSomething")

        assertTrue(loaded.declaredMethods.any { it.name == methodName })
        assertTrue(loaded.declaredFields.any { it.name == fieldName })
    }

    object TestHelpers: VariableFramesIntrinsics {
        override fun beginFrame(mediator: Any, capacity: Int) {

        }

        override fun set(mediator: Any, index: Int, value: Any?) {

        }
    }

    object TestHelpers2: TriggerItemIntrinsics {
        override fun walk(trigger: Any?, event: Any) {
        }
    }
}
