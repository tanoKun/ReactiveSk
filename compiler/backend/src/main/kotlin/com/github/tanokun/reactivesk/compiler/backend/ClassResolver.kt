package com.github.tanokun.reactivesk.compiler.backend

import com.github.tanokun.reactivesk.lang.Identifier
import net.bytebuddy.description.type.TypeDescription

interface ClassResolver {

    /**
     * 指定された型名に対応する [TypeDescription] を解決します。
     * また、動的クラスが一番優先されます。
     *
     * @param typeName 解決する型の名前を表す [Identifier]
     * @param isArray 型が配列であるかどうかを示すフラグ。デフォルトは `false`
     *
     * @return 対応する [TypeDescription]
     */
    fun resolveTypeDescription(typeName: Identifier, isArray: Boolean = false): TypeDescription
}