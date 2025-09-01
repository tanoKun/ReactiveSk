package com.github.tanokun.addon.intermediate.integrity;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

public class SetValueAdvice {

    @Advice.OnMethodEnter
    public static <T> void enter(
            @Advice.Argument(0) T buf,
            @Advice.FieldValue(readOnly = false, typing = Assigner.Typing.DYNAMIC) T field
    ) {
        field = buf;
    }
}
