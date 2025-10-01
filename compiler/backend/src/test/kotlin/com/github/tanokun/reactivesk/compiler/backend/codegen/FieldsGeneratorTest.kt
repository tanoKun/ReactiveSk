package com.github.tanokun.reactivesk.compiler.backend.codegen

import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.compiler.backend.codegen.field.FieldsGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFieldOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalSetterOf
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.JvmSetterIntrinsics
import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.lang.PropertyModifiers
import io.mockk.every
import io.mockk.mockk
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.dynamic.scaffold.TypeValidation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.test.Test

class FieldsGeneratorTest {
    val classResolver = mockk<ClassResolver> {
        every { resolveTypeDescription(Identifier("String"), false) } returns TypeDescription.ForLoadedType.of(String::class.java)
        every { resolveTypeDescription(Identifier("String"), true) } returns TypeDescription.ForLoadedType.of(ArrayList::class.java)
    }

    val generator = FieldsGenerator(classResolver, TestHelpers::class.java)

    @Test
    @DisplayName("フィールドとセッターが生成される")
    fun defineFieldsActuallyGeneratesFieldAndNonArraySetter() {
        val prop = mockk<ClassDefinition.Property> {
            every { propertyName } returns Identifier("field1")
            every { typeName } returns Identifier("String")
            every { isArray } returns false
            every { modifiers } returns PropertyModifiers.PUBLIC
        }

        val prop2 = mockk<ClassDefinition.Property> {
            every { propertyName } returns Identifier("field2")
            every { typeName } returns Identifier("String")
            every { isArray } returns false
            every { modifiers } returns (PropertyModifiers.FACTOR or PropertyModifiers.PUBLIC)
        }

        val prop3 = mockk<ClassDefinition.Property> {
            every { propertyName } returns Identifier("arrField1")
            every { typeName } returns Identifier("String")
            every { isArray } returns true
            every { modifiers } returns PropertyModifiers.PUBLIC
        }

        val prop4 = mockk<ClassDefinition.Property> {
            every { propertyName } returns Identifier("arrField2")
            every { typeName } returns Identifier("String")
            every { isArray } returns true
            every { modifiers } returns (PropertyModifiers.FACTOR or PropertyModifiers.PUBLIC)
        }

        val classDefinition = mockk<ClassDefinition>(relaxed = true)
        every { classDefinition.getProperties() } returns listOf(prop, prop2, prop3, prop4)

        val builder = ByteBuddy(ClassFileVersion.JAVA_V8)
            .with(TypeValidation.DISABLED)
            .subclass(Any::class.java)
            .name("generated.TestGeneratedFields1")

        val resultBuilder = generator.defineFields(builder, classDefinition)

        val dynamicType = resultBuilder.make()
        val loaded = dynamicType
            .load(this::class.java.classLoader, ClassLoadingStrategy.Default.WRAPPER)
            .apply { saveIn(File("generated-classes")) }
            .loaded

        val fieldName = internalFieldOf("field1")
        val setterName = internalSetterOf("field1")

        assertTrue(loaded.declaredFields.any { it.name == fieldName })
        assertTrue(loaded.declaredMethods.any { it.name == setterName })
    }

    object TestHelpers: JvmSetterIntrinsics {

        override fun notifyChanged(notified: Boolean, instance: Any, propertyName: String, oldValue: Any?, newValue: Any?) {
        }

        override fun checkTypes(list: ArrayList<*>, expected: Class<*>) {
        }
    }
}