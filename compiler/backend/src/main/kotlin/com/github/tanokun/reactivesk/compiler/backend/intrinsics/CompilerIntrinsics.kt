package com.github.tanokun.reactivesk.compiler.backend.intrinsics

import java.lang.reflect.Field
import java.lang.reflect.Method

val Class<out TriggerItemIntrinsics>.TRIGGER_ITEM_INSTANCE_FIELD: Field
    get() = this.getField("INSTANCE")

fun Class<out TriggerItemIntrinsics>.getWalkMethod(): Method =
    this.getMethod("walk", Any::class.java, Any::class.java)

fun Class<out VariableFramesIntrinsics>.getBeginFrameMethod(): Method =
    this.getMethod("beginFrame", Any::class.java, Int::class.java)

fun Class<out VariableFramesIntrinsics>.getSetMethod(): Method =
    this.getMethod("set", Any::class.java, Int::class.java, Any::class.java)

val Class<out VariableFramesIntrinsics>.VARIABLE_FRAMES_INSTANCE_FIELD: Field
    get() = this.getField("INSTANCE")

fun Class<out JvmSetterIntrinsics>.getNotifyChangedMethod(): Method =
    this.getMethod("notifyChanged", Boolean::class.java, Any::class.java, String::class.java, Any::class.java, Any::class.java)

fun Class<out JvmSetterIntrinsics>.getCheckTypesMethod(): Method =
    this.getMethod("checkTypes", ArrayList::class.java, Class::class.java)

val Class<out JvmSetterIntrinsics>.JVM_SETTER_INSTANCE_FIELD: Field
    get() = this.getField("INSTANCE")