package com.github.tanokun.addon.intermediate.integrity;

import com.github.tanokun.addon.runtime.notfiy.ChangeNotifier;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

public class SetAndNotifyValueAdvice {

    @Advice.OnMethodEnter
    public static <T> void enter(
            @Advice.This Object obj,
            @FieldName String name,
            @Advice.Argument(0) T buf,
            @Advice.Argument(1) boolean shouldNotify,
            @Advice.FieldValue(readOnly = false, typing = Assigner.Typing.DYNAMIC) T field
    ) {
        T old = field;
        field = buf;

        if (old == field) return;
        if (!shouldNotify) return;

        ChangeNotifier.notify(obj, old, field, name);
    }
}
