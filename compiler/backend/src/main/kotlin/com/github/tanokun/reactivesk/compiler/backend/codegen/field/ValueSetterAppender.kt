package com.github.tanokun.reactivesk.compiler.backend.codegen.field

import com.github.tanokun.reactivesk.compiler.backend.intrinsics.JvmSetterIntrinsics
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.getNotifyChangedMethod
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.implementation.bytecode.StackManipulation
import net.bytebuddy.implementation.bytecode.constant.TextConstant
import net.bytebuddy.implementation.bytecode.member.FieldAccess
import net.bytebuddy.implementation.bytecode.member.MethodInvocation
import net.bytebuddy.implementation.bytecode.member.MethodReturn
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.matcher.ElementMatchers

class ValueSetterAppender(
    private val fieldName: String,
    private val propertyNameLiteral: String,
    private val isFactor: Boolean,
    intrinsicsObjectClass: Class<out JvmSetterIntrinsics>,
) : ByteCodeAppender {
    private val instanceFieldDescription: FieldDescription.InDefinedShape =
        TypeDescription.ForLoadedType.of(intrinsicsObjectClass)
            .declaredFields
            .filter(ElementMatchers.named("INSTANCE"))
            .single()

    private val notifyChangedMethodDescription = MethodDescription.ForLoadedMethod(
        intrinsicsObjectClass.getNotifyChangedMethod()
    )

    override fun apply(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ): ByteCodeAppender.Size {
        val instrumentedType: TypeDescription = instrumentedMethod.declaringType.asErasure()

        val fieldDescription: FieldDescription.InDefinedShape =
            instrumentedType.declaredFields
                .filter(ElementMatchers.named(fieldName))
                .single()

        val stack = StackManipulation.Compound(
            *if (isFactor) createEvacuationValueStack(fieldDescription) else arrayOf(),
            MethodVariableAccess.loadThis(),
            MethodVariableAccess.REFERENCE.loadFrom(1),
            FieldAccess.forField(fieldDescription).write(),

            *if (isFactor) createNotifyChangedStack() else arrayOf(),

            MethodReturn.VOID
        )

        val applied = stack.apply(methodVisitor, implementationContext)

        return ByteCodeAppender.Size(applied.maximalSize, instrumentedMethod.stackSize + 1)
    }

    // String old = this.field
    private fun createEvacuationValueStack(fieldDescription: FieldDescription): Array<StackManipulation> =
        arrayOf(
            MethodVariableAccess.loadThis(),
            FieldAccess.forField(fieldDescription).read(),
            MethodVariableAccess.REFERENCE.storeAt(3)
        )

    // JvmSetterIntrinsics.INSTANCE.notifyChanged(var2, this, "field2", old, var1)
    private fun createNotifyChangedStack(): Array<StackManipulation> =
        arrayOf(
            FieldAccess.forField(instanceFieldDescription).read(),   // push INSTANCE (レシーバ)
            MethodVariableAccess.INTEGER.loadFrom(2),                // boolean notified
            MethodVariableAccess.loadThis(),                         // this
            TextConstant(propertyNameLiteral),                       // "field2"
            MethodVariableAccess.REFERENCE.loadFrom(3),              // old
            MethodVariableAccess.REFERENCE.loadFrom(1),              // new
            MethodInvocation.invoke(notifyChangedMethodDescription), // INVOKEVIRTUAL
        )
}