package com.github.tanokun.reactivesk.compiler.backend.metadata

import com.github.tanokun.reactivesk.lang.PropertyModifier

/**
 * 修飾子に関する情報を管理するアノテーションです。
 *
 * @param modifiers 修飾子を表す整数値
 */
annotation class ModifierMetadata(val modifiers: PropertyModifier)
