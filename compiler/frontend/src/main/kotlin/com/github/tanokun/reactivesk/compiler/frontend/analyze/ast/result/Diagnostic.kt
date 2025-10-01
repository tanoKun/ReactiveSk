package com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.result

import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode

/**
 * 解析中に発見された問題点（エラーや警告）を表す。
 */
data class Diagnostic<T>(
    val message: String,
    val location: AstNode<T>,
    val severity: Severity = Severity.ERROR
)