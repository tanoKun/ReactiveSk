package com.github.tanokun.addon.definition

data class Identifier(val identifier: String) {
    override fun toString(): String = identifier

    companion object {
        val empty = Identifier("")
    }
}