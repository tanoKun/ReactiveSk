package com.github.tanokun.addon.definition.variable

import ch.njol.skript.config.Node
import com.github.tanokun.addon.definition.Identifier

object TypedVariableResolver {
    private val declarationStore = hashMapOf<Node, HashMap<Int, ArrayList<TypedVariableDeclaration>>>()

    /**
     * 型付き変数宣言をスコープ単位で検証・初期化します。
     *
     * @param topNode 変数を管理するノードコンテキスト
     * @param declaration 追加を試みる型付き変数の宣言
     *
     * @throws IllegalArgumentException scope が負
     * @throws IllegalArgumentException 同一スコープ内に同一宣言が既に存在する場合
     */

    fun addDeclaration(topNode: Node, declaration: TypedVariableDeclaration) {
        val declarationsInScope = declarationStore
            .computeIfAbsent(topNode) { hashMapOf() }
            .computeIfAbsent(declaration.scopeCount) { arrayListOf() }

        if (declarationsInScope.contains(declaration)) {
            throw IllegalArgumentException("Typed variable '$declaration' is already declared in this scope.")
        }

        declarationsInScope.add(declaration)
    }

    /**
     * 指定したトップノード下で、与えられたスコープから外側へ向かって
     * 名前に一致する型付き変数宣言を探索して返します。
     *
     * 探索は [scopeCount] から 0 まで降順に行われ、最初に見つかった宣言を返します。
     * トップノードに対応する宣言テーブルが存在しない場合、または一致する宣言が
     * 見つからない場合は null を返します。
     *
     * [scopeCount] は 0 以上を想定しています。負の値を渡した場合、探索は行われません。
     *
     * @param topNode 変数宣言を管理するトップノード
     * @param scopeCount 探索開始スコープ(値が大きいほど内側のスコープ)
     * @param name 探索する変数名
     * @return 一致する最初の宣言、存在しない場合は null
     */
    fun getDeclarationInScopeChain(topNode: Node, scopeCount: Int, name: Identifier): TypedVariableDeclaration? {
        val declarations = declarationStore[topNode] ?: return null

        for (i in scopeCount downTo 0) {
            declarations[i]?.firstOrNull { it.variableName == name }?.let { return it }
        }

        return null
    }

    fun getDeclarationInSingleScope(topNode: Node, scopeCount: Int, name: Identifier): TypedVariableDeclaration? {
        val declarations = declarationStore[topNode] ?: return null

        return declarations[scopeCount]?.firstOrNull { it.variableName == name }
    }

    fun clearAllDeclarations() { declarationStore.clear() }
}