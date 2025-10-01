package com.github.tanokun.addon.intermediate.reduce;

import ch.njol.skript.lang.TriggerItem;
import net.bytebuddy.asm.Advice;
import org.bukkit.event.Event;

import static com.github.tanokun.addon.intermediate.generator.ClassBodyMetadataKt.FUNCTION_TRIGGER_PREFIX;

public class FunctionAdvice {
    @Advice.OnMethodExit
    public static void exit(
            @Advice.FieldValue(FUNCTION_TRIGGER_PREFIX) TriggerItem item,
            @Advice.Argument(0) Object event
    ) {
        if (item != null) {
            TriggerItem.walk(item, (Event) event);
        }
    }
}
