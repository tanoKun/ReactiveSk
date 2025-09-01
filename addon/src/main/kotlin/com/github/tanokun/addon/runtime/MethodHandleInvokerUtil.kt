package com.github.tanokun.addon.runtime

import java.lang.invoke.MethodHandle

object MethodHandleInvokerUtil {
    fun <E> buildConstructor(argumentSize: Int, methodHandle: MethodHandle): (Array<*>, E) -> Any {
        return when (argumentSize) {
            0 -> { _, e -> methodHandle.invokeExact(e) }
            1 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0]) }
            2 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1]) }
            3 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2]) }
            4 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3]) }
            5 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]) }
            6 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5]) }
            7 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6]) }
            8 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7]) }
            9 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8]) }
            10 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9]) }
            11 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10]) }
            12 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11]) }
            13 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12]) }
            14 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13]) }
            15 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14]) }
            16 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15]) }
            17 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16]) }
            18 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16], arguments[17]) }
            19 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16], arguments[17], arguments[18]) }
            20 -> { arguments, e -> methodHandle.invokeExact(e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16], arguments[17], arguments[18], arguments[19]) }
            else -> throw IllegalArgumentException("Invalid argument size: $argumentSize")
        }
    }

    fun buildFunction(argumentSize: Int, methodHandle: MethodHandle): (Array<Any?>, Any, Any) -> Unit {
        return when (argumentSize) {
            0 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e) }
            1 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0]) }
            2 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1]) }
            3 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2]) }
            4 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3]) }
            5 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]) }
            6 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5]) }
            7 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6]) }
            8 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7]) }
            9 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8]) }
            10 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9]) }
            11 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10]) }
            12 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11]) }
            13 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12]) }
            14 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13]) }
            15 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14]) }
            16 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15]) }
            17 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16]) }
            18 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16], arguments[17]) }
            19 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16], arguments[17], arguments[18]) }
            20 -> { arguments, callee, e -> methodHandle.invokeExact(callee, e, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16], arguments[17], arguments[18], arguments[19]) }
            else -> throw IllegalArgumentException("Invalid argument size: $argumentSize")
        }
    }
}
