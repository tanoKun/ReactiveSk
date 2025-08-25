package com.github.tanokun.addon.instance

abstract class AnyInstance {
    var className: String = ""
    var properties: Collection<InstanceProperty> = arrayListOf()

    override fun toString(): String {
        return "Instance(className='$className', properties=$properties)"
    }
}