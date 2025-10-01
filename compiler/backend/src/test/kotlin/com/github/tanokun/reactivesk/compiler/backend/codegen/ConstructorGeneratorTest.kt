package com.github.tanokun.reactivesk.compiler.backend.codegen

import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.compiler.backend.codegen.constructor.ConstructorGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.CONSTRUCTOR_PROXY
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.CONSTRUCTOR_TRIGGER_SECTION
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.TriggerItemIntrinsics
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.VariableFramesIntrinsics
import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.Identifier
import io.mockk.every
import io.mockk.mockk
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertNotNull

class ConstructorGeneratorTest {
    @Test
    @DisplayName("コンストラクタ生成のテスト")
    fun generateConstructorTest() {
        val classResolver = mockk<ClassResolver> {
            every { resolveTypeDescription(Identifier("String"), false) } returns TypeDescription.ForLoadedType(String::class.java)
            every { resolveTypeDescription(any(), true) } returns TypeDescription.ForLoadedType(ArrayList::class.java)
        }

        val constructorHolder = mockk<ClassDefinition.Constructor> {
            every { parameters } returns listOf(
                ClassDefinition.Constructor.Parameter.NotProperty(
                    Identifier("argument1"), Identifier("String"), isArray = false
                ),
                ClassDefinition.Constructor.Parameter.NotProperty(
                    Identifier("argument2"), Identifier("String"), isArray = false
                ),
                ClassDefinition.Constructor.Parameter.NotProperty(
                    Identifier("argument3"), Identifier("String"), isArray = true
                ),
            )
        }

        val classDefinition = mockk<ClassDefinition> {
            every { constructor } returns constructorHolder
        }

        val generator = ConstructorGenerator(
            classResolver,
            TestHelpers::class.java,
            TestHelpers2::class.java,
            isImplementingBeginFrame = true
        )

        val builder = ByteBuddy()
            .subclass(Any::class.java)
            .name("generated.TestGeneratedClass")

        val resultBuilder = generator
            .defineConstructor(builder, classDefinition)


        val dynamicType = resultBuilder.make()
        val loaded = dynamicType
            .load(this::class.java.classLoader, ClassLoadingStrategy.Default.WRAPPER)
            .apply { saveIn(File("generated-classes")) }
            .loaded

        val hasField = loaded.declaredFields.any { it.name == CONSTRUCTOR_TRIGGER_SECTION }
        val hasMethod = loaded.declaredMethods.any { it.name == CONSTRUCTOR_PROXY }
        assertNotNull(loaded)

        assert(hasField || hasMethod)
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