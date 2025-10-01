package com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode

sealed class AstParseResult {
    /**
     * 解析結果として単一のAstSectionと次に処理すべきTriggerItemを返す
     */
    data class Single(
        val astNode: AstNode<TriggerItem>,
        val nextItem: TriggerItem?
    ) : AstParseResult()
    
    /**
     * 解析をスキップして、標準のLine処理に委譲する
     */
    object Skip : AstParseResult()
}
