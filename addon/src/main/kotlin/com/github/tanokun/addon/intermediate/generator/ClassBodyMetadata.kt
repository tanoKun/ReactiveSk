package com.github.tanokun.addon.intermediate.generator

/**
 * 内部シンボルの命名規約・定数を一元管理。
 * - 可読性と一意性を両立した接頭辞に統一
 */
private const val INTERNAL_PREFIX: String = $$"rSk$internal$"
private const val LOCALS_CAPACITY_PREFIX: String = INTERNAL_PREFIX + "capacity$"
const val FIELD_PREFIX: String = INTERNAL_PREFIX + "field$"
const val CONSTRUCTOR_TRIGGER_SECTION: String = INTERNAL_PREFIX + "init"
const val CONSTRUCTOR_PROXY: String = INTERNAL_PREFIX + $$"proxy$ctor"
const val CONSTRUCTOR_LOCALS_CAPACITY: String = LOCALS_CAPACITY_PREFIX + "ctor"
const val FUNCTION_TRIGGER_PREFIX: String = INTERNAL_PREFIX + $$"trigger$fun$"
private const val FUNCTION_NAME_PREFIX: String = INTERNAL_PREFIX + "fun$"
/** 関数トリガー用の内部フィールド名を生成 */
fun internalFunctionTriggerField(funcName: String): String = "$FUNCTION_TRIGGER_PREFIX$funcName"

fun internalFunctionNameOf(funcName: String): String = "$FUNCTION_NAME_PREFIX$funcName"

fun internalArrayListSetterOf(name: String): String =
    INTERNAL_PREFIX + "set" + name.replaceFirstChar(Char::uppercase) + "ArrayList"

fun internalSetterOf(name: String): String =
    INTERNAL_PREFIX + "set" + name.replaceFirstChar(Char::uppercase)

fun internalFieldOf(name: String): String = "$FIELD_PREFIX$name"

fun internalFunctionLocalsCapacityFieldOf(functionName: String): String =
    LOCALS_CAPACITY_PREFIX + "fun$" + functionName
