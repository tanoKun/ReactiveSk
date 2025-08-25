package com.github.tanokun.addon.clazz.definition.parse

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class ParserErrorListener : BaseErrorListener() {

    val errors = mutableListOf<String>()

    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {
        errors.add(msg)
    }

    fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }
}