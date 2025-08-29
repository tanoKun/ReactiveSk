package com.github.tanokun.addon.intermediate.reduce;

import ch.njol.skript.lang.TriggerItem;
import net.bytebuddy.asm.Advice;

import static com.github.tanokun.addon.intermediate.generator.ClassBodyMetadataKt.INTERNAL_INIT_TRIGGER_SECTION;

public class InitAdvice {
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(
            @Advice.FieldValue(INTERNAL_INIT_TRIGGER_SECTION) TriggerItem item
    ) {
        return item == null;
    }
}
