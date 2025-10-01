package com.github.tanokun.reactivesk.compiler.frontend.analyze.variable

import com.github.tanokun.reactivesk.lang.Identifier

/**
 * 型付き変数宣言を表すデータクラス。
 * 変数名、型、可変性、スコープの深さ、インデックスを保持します。
 * また、インデックスは解決時に自動的に変更されます。
 *
 * @param variableName 変数名を表す [Identifier]
 * @param type 変数の型を表す [Class]
 * @param isMutable 変数が可変かどうかを示す
 * @param depth 変数が宣言されたスコープの深さ(0起点)
 * @param index 変数のインデックス
 */
sealed interface TypedVariableDeclaration {
    val variableName: Identifier
    val type: Class<*>
    val isMutable: Boolean
    val depth: Int
    val index: Int

    data class Unresolved(
        override val variableName: Identifier,
        override val type: Class<*>,
        override val isMutable: Boolean,
        override val depth: Int,
    ) : TypedVariableDeclaration {
        override val index: Int = -1

        fun resolve(newIndex: Int): Resolved =
            Resolved(variableName = variableName, type = type, isMutable = isMutable, depth = depth, index = newIndex)
    }

    data class Resolved(
        override val variableName: Identifier,
        override val type: Class<*>,
        override val isMutable: Boolean,
        override val depth: Int,
        override val index: Int,
    ) : TypedVariableDeclaration
}