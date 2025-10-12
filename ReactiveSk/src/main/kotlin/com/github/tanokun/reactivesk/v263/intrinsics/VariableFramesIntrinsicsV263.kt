package com.github.tanokun.reactivesk.v263.intrinsics

import com.github.tanokun.reactivesk.compiler.backend.intrinsics.VariableFramesIntrinsics
import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames

/**
 * 変数フレーム操作のための [VariableFramesIntrinsics] 実装を提供します。
 *
 * 内部で [AmbiguousVariableFrames] を経由して実際の操作を行います。
 */
object VariableFramesIntrinsicsV263: VariableFramesIntrinsics {

    /**
     * [AmbiguousVariableFrames] を経由して変数フレームの値を設定します。
     *
     * @param mediator 実際の変数フレームを仲介するオブジェクト ([AmbiguousVariableFrames] の内部実装に依存)
     * @param index 設定する変数のインデックス
     * @param value 設定する値
     */
    override fun set(mediator: Any, index: Int, value: Any?) {
        AmbiguousVariableFrames.set(mediator, index, value)
    }
}