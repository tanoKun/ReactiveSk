package com.github.tanokun.addon.intermediate.generator

import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.module.ModuleManager
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.scaffold.TypeValidation
import java.lang.reflect.Modifier

class ByteBuddyGenerator(private val moduleManager: ModuleManager) {
    fun generateClass(classDefinition: ClassDefinition): DynamicType.Unloaded<*> {
        val fqcn = "com.github.tanokun.addon.generated.${classDefinition.className.identifier}"

        val fieldsDefiner = FieldsDefiner(moduleManager)
        val methodsDefiner = MethodsDefiner(moduleManager)
        val constructorDefiner = ConstructorDefiner(moduleManager)

        var builder: DynamicType.Builder<*> = ByteBuddy(ClassFileVersion.JAVA_V8)
            .with(TypeValidation.DISABLED)
            .subclass(TypeDescription.ForLoadedType.of(DynamicClass::class.java))
            .name(fqcn)
            .modifiers(Modifier.PUBLIC)

        builder = fieldsDefiner.defineFields(builder, classDefinition)
        builder = methodsDefiner.defineAllMethods(builder, classDefinition)
        builder = constructorDefiner.defineConstructor(builder, classDefinition)

        return builder.make()
    }
}