package com.github.tanokun.reactivesk.skriptadapter.common.dynamic

import com.github.tanokun.reactivesk.lang.Identifier

sealed interface ChangedDifference {
    val className: Identifier

    data class Added(override val className: Identifier, val new: Class<*>) : ChangedDifference
    data class Removed(override val className: Identifier, val previous: Class<*>) : ChangedDifference
    data class Changed(override val className: Identifier, val previous: Class<*>, val new: Class<*>) : ChangedDifference
}