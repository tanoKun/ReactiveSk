package com.github.tanokun.reactivesk.compiler.backend.codegen

import com.github.tanokun.reactivesk.compiler.backend.codegen.constructor.ConstructorGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.field.FieldsGenerator
import com.github.tanokun.reactivesk.compiler.backend.codegen.method.MethodsGenerator
import com.github.tanokun.reactivesk.lang.ClassDefinition
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.scaffold.TypeValidation
import java.lang.reflect.Modifier

class JvmBytecodeGenerator(
    val superClass: Class<*>,
    private val constructorGenerator: ConstructorGenerator,
    private val methodsGenerator: MethodsGenerator,
    private val fieldsGenerator: FieldsGenerator,
) {
    private fun generateFQCN(classDefinition: ClassDefinition): String =
        "com.github.tanokun.reactivesk.generated.${classDefinition.className}"

    fun generateClass(classDefinition: ClassDefinition): DynamicType.Unloaded<*> {
        val fqcn = generateFQCN(classDefinition)

        var builder: DynamicType.Builder<*> = ByteBuddy(ClassFileVersion.JAVA_V8)
            .with(TypeValidation.DISABLED)
            .subclass(TypeDescription.ForLoadedType.of(superClass))
            .name(fqcn)
            .modifiers(Modifier.PUBLIC)

        builder = fieldsGenerator.defineFields(builder, classDefinition)
        builder = methodsGenerator.defineAllMethods(builder, classDefinition)
        builder = constructorGenerator.defineConstructor(builder, classDefinition)

        return builder.make()
    }
}