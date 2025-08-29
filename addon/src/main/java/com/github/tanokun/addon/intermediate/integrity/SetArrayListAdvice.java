package com.github.tanokun.addon.intermediate.integrity;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

public class SetArrayListAdvice {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface FixedArrayComponentType {}

    @Advice.OnMethodEnter
    public static void enter(
            @Advice.Argument(0) ArrayList<Object> list,
            @Advice.FieldValue(readOnly = false, typing = Assigner.Typing.DYNAMIC) ArrayList<?> arrayField,
            @FixedArrayComponentType Class<?> componentType
    ) {
        if (list == null) throw new IllegalArgumentException("list is null");

        ArrayList<Object> buf = new ArrayList<>(list.size());
        for (Object elem : list) {
            if (!componentType.isInstance(elem)) throw new IllegalArgumentException("array element type is not " + componentType.getName() + ": " + elem.toString());

            buf.add(elem);
        }

        arrayField = buf;
    }
}
