package com.github.tanokun.addon.intermediate.generator.helper

import com.github.tanokun.addon.runtime.notfiy.ChangeNotifier

object SetListHelper {

    @JvmStatic
    fun checkTypes(list: ArrayList<*>, clazz: Class<*>) {
        for (elem in list) {
            require(clazz.isInstance(elem)) { "array element type is not " + clazz.simpleName + ": " + elem.toString() }
        }
    }

    @JvmStatic
    fun notify(obj: Any, oldValue: ArrayList<*>, newValue: ArrayList<*>, reason: String) = ChangeNotifier.notify(obj, oldValue, newValue, reason)
}