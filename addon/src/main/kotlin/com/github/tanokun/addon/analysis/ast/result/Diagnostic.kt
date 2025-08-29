package com.github.tanokun.addon.analysis.ast.result

import com.github.tanokun.addon.analysis.ast.AstSection

/**
 * 解析中に発見された問題点（エラーや警告）を表す。
 */
data class Diagnostic(
    val message: String,
    val location: AstSection,
    val severity: Severity = Severity.ERROR
)