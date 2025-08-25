package com.github.tanokun.addon.clazz.invoke

import net.bytebuddy.implementation.bind.annotation.AllArguments
import net.bytebuddy.implementation.bind.annotation.Origin
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.This
import java.lang.reflect.Method

class DynamicInvokeFunction {
    @RuntimeType
    fun execute(
        @This instance: Any,
        @Origin method: Method,
        @AllArguments args: Array<Any?>
    ): Any? {
        println("--- StaticFunction.execute ---")
        println("Instance Class: ${instance.javaClass.name}")
        println("Called Method: ${method.name}")
        println("Arguments: ${args.joinToString { it?.toString() ?: "null" }}")

        println("------------------------------")

        return "aaa"
    }
}