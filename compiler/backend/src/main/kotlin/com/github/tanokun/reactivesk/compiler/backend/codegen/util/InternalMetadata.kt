package com.github.tanokun.reactivesk.compiler.backend.codegen.util

/**
 * 内部シンボルの命名規約・定数を一元管理。
 * - 可読性と一意性を両立した接頭辞に統一
 */
private const val INTERNAL_PREFIX: String = $$"rSk$internal$"
const val FIELD_PREFIX: String = INTERNAL_PREFIX + "field$"
const val CONSTRUCTOR_TRIGGER_SECTION: String = INTERNAL_PREFIX + "init"
const val CONSTRUCTOR_PROXY: String = INTERNAL_PREFIX + $$"proxy$ctor"
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