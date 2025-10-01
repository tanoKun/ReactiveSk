package com.github.tanokun.reactivesk.addon.module

import com.github.tanokun.reactivesk.addon.skriptAdapter
import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.lang.Identifier
import net.bytebuddy.description.annotation.AnnotationList
import net.bytebuddy.description.type.TypeDescription
import java.lang.reflect.Modifier

class ModuleClassResolver(private val moduleManager: PluginDynamicManager): ClassResolver {
    override fun resolveTypeDescription(typeName: Identifier, isArray: Boolean): TypeDescription {
        if (isArray) return TypeDescription.ForLoadedType.of(ArrayList::class.java)
        if (typeName.identifier == "void") return TypeDescription.ForLoadedType.of(Void.TYPE)

        moduleManager.getLoadedClass(typeName)?.let { return TypeDescription.ForLoadedType.of(it) }
        skriptAdapter.getSkriptClass(typeName)?.let { return TypeDescription.ForLoadedType.of(it) }

        val fqcn = "com.github.tanokun.addon.generated.$typeName"

        return SafeLatentTypeDescription(fqcn)
    }

    internal class SafeLatentTypeDescription(name: String) : TypeDescription.Latent(
        name,
        Modifier.PUBLIC,
        TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Any::class.java),
        emptyList()
    ) {
        override fun getDeclaringType(): TypeDescription = this
        override fun getInnerClassCount(): Int = 0
        override fun getDeclaredAnnotations(): AnnotationList = AnnotationList.Empty()
    }
}