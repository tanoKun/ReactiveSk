package com.github.tanokun.addon.maker

import com.github.tanokun.addon.maker.function.NonSuspendFunctionRuntimeBukkitEvent
import kotlin.test.Test

class FunctionRuntimeBukkitEventTest {
    @Test
    fun test() {
        println(NonSuspendFunctionRuntimeBukkitEvent() == NonSuspendFunctionRuntimeBukkitEvent())
    }
}