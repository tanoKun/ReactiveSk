package com.github.tanokun.addon.definition

import ch.njol.skript.classes.ClassInfo
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.dynamic.DynamicClass

/**
 * 動的クラスの [ClassInfo] をラップします。
 */
data class DynamicClassInfo(val classDefinition: ClassDefinition, val classInfo: Class<out DynamicClass>)