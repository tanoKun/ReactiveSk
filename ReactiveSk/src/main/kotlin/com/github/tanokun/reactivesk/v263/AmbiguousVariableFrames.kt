package com.github.tanokun.reactivesk.v263

import java.util.*

/**
 * あいまいな変数フレームを管理するためのユーティリティオブジェクトです。
 *
 * 各媒介オブジェクトに対して可変長の値リストを保持し、値の取得・設定およびフレームの終了を行います。
 */
object AmbiguousVariableFrames {
    private val frames: WeakHashMap<Any, ArrayList<Any?>> = WeakHashMap()

    /**
     * 指定した [mediator] に紐づく変数フレームを終了し、内部状態を解放します。
     *
     * @param mediator 終了対象の媒介オブジェクト
     */
    fun endFrame(mediator: Any) {
        frames.remove(mediator)
    }

    /**
     * 指定した [mediator] の変数フレームからインデックス [index] に対応する値を取得します。
     *
     * @param mediator 値を取得する対象の媒介オブジェクト
     * @param index 取得する値のインデックス
     *
     * @return 指定したインデックスの値が存在する場合はその値 存在しない場合は null
     */
    fun get(mediator: Any, index: Int): Any? {
        val list = frames[mediator] ?: return null

        return if (index >= 0 && index < list.size) list[index] else null
    }

    /**
     * 指定した [mediator] の変数フレームに対してインデックス [index] の位置に [value] を設定します。
     * 必要に応じて内部リストの容量を拡張します。
     *
     * @param mediator 値を設定する対象の媒介オブジェクト
     * @param index 設定する変数のインデックス
     * @param value 設定する値
     */
    fun set(mediator: Any, index: Int, value: Any?) {
        var list = frames[mediator]
        if (list == null) {
            list = ArrayList<Any?>(maxOf(8, index + 1))
            frames[mediator] = list
        }

        if (index >= list.size) {
            list.ensureCapacity(index + 1)
            while (list.size <= index) list.add(null)
        }

        list[index] = value
    }
}
