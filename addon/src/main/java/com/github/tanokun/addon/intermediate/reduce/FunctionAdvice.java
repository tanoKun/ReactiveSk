package com.github.tanokun.addon.intermediate.reduce;

import ch.njol.skript.lang.TriggerItem;
import net.bytebuddy.asm.Advice;

import static com.github.tanokun.addon.intermediate.generator.ClassBodyMetadataKt.INTERNAL_FUNCTION_TRIGGER_PREFIX;

public class FunctionAdvice {
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(
            @Advice.FieldValue(INTERNAL_FUNCTION_TRIGGER_PREFIX) TriggerItem item
    ) {
        return item == null;
    }
}
