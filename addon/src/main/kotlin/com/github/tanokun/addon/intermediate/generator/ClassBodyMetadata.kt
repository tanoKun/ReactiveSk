package com.github.tanokun.addon.intermediate.generator

import java.util.*

/**
 * 内部シンボルの命名規約・定数を一元管理。
 * - 可読性と一意性を両立した接頭辞に統一
 */
const val FIELD_PREFIX: String = $$"rSk$field$"
private const val INTERNAL_PREFIX: String = $$"rSk$internal$"
const val INTERNAL_FUNCTION_TRIGGER_PREFIX: String = INTERNAL_PREFIX + "fun$"
private const val INTERNAL_FUNCTION_RETURN_TYPE_PREFIX: String = INTERNAL_PREFIX + $$"fun$return$"
const val INTERNAL_INIT_TRIGGER_SECTION: String = INTERNAL_PREFIX + "init"
const val INTERNAL_CONSTRUCTOR_PROXY: String = INTERNAL_PREFIX + $$"proxy$ctor"
private const val INTERNAL_LOCALS_CAPACITY_PREFIX: String = INTERNAL_PREFIX + "capacity$"
const val INTERNAL_CONSTRUCTOR_LOCALS_CAPACITY: String = INTERNAL_LOCALS_CAPACITY_PREFIX + "ctor"
/** 関数トリガー用の内部フィールド名を生成 */
fun internalFunctionTriggerField(funcName: String): String = "$INTERNAL_FUNCTION_TRIGGER_PREFIX$funcName"

/** ArrayList セッタ用の内部メソッド名を生成: INTERNAL_PREFIX + set + UpperCamel(name) + ArrayList */
fun internalArrayListSetterOf(name: String): String =
    INTERNAL_PREFIX + "set" + name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
    } + "ArrayList"

fun internalFunctionReturnTypeField(funcName: String): String =
    "$INTERNAL_FUNCTION_RETURN_TYPE_PREFIX$funcName"

fun fieldOf(name: String): String = "$FIELD_PREFIX$name"

fun internalLocalsCapacityFieldOfFunction(functionName: String): String =
    INTERNAL_LOCALS_CAPACITY_PREFIX + "fun$" + functionName
