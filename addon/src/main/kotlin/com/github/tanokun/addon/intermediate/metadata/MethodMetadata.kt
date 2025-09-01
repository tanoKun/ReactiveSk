package com.github.tanokun.addon.intermediate.metadata

import kotlin.reflect.KClass

annotation class MethodMetadata(val returnType: KClass<*>, val argumentTypes: Array<KClass<*>>, val modifiers: Int)
