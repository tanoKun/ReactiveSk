package com.github.tanokun.addon.runtime

abstract class AnyInstance {
    var className: String = ""
    var properties: Collection<InstanceProperty> = arrayListOf()

    override fun toString(): String {
        return "Instance(className='$className', properties=$properties)"
    }
}