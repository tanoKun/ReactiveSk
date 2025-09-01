package com.github.tanokun.addon.intermediate

import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.lang.TriggerSection
import java.lang.reflect.Field


/**
 * 汎用のSectionから最初のTriggerItemを取得する拡張プロパティ。
 */
val TriggerSection.firstInSection: TriggerItem?
    get() {
        val field = Reflection.findField(this.javaClass, "first")
        return field.get(this) as? TriggerItem
    }

/**
 * リフレクションによるフィールドアクセスをカプセル化し、結果をキャッシュするシングルトンオブジェクト。
 */
object Reflection {
    private val fieldCache = mutableMapOf<Pair<Class<*>, String>, Field>()

    fun findField(cls: Class<*>, name: String): Field = fieldCache.getOrPut(cls to name) {
        var currentClass: Class<*>? = cls
        while (currentClass != null) {
            try {
                return@getOrPut currentClass.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        throw NoSuchFieldException("Field '$name' not found in ${cls.name} or its superclasses")
    }
}