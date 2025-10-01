package com.github.tanokun.reactivesk.compiler.backend.intrinsics

interface JvmSetterIntrinsics {
    /**
     * プロパティの値が変更されたことを通知します。
     *
     * @param notified 通知が必要かどうか
     * @param instance プロパティを持つインスタンス
     * @param propertyName プロパティ名
     * @param oldValue 古い値
     * @param newValue 新しい値
     */
    fun notifyChanged(notified: Boolean, instance: Any, propertyName: String, oldValue: Any?, newValue: Any?)

    /**
     * list の中身がすべて expected と同じ型かどうかをチェックします。
     *
     * @param list チェックするリスト
     * @param expected 期待する型
     *
     * @throws IllegalArgumentException 型が異なる場合
     */
    fun checkTypes(list: ArrayList<*>, expected: Class<*>)
}