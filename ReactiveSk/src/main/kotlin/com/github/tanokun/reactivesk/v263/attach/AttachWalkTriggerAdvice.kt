package com.github.tanokun.reactivesk.v263.attach

import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames
import net.bytebuddy.asm.Advice
import org.bukkit.event.Event

/**
 * トリガーの walk 実行後に変数フレームを終了します。
 */
object AttachWalkTriggerAdvice {
    @JvmStatic
    @Advice.OnMethodExit(onThrowable = Throwable::class)
    /**
     * トリガーの walk 実行後に呼び出され、対応する変数フレームを終了します。
     *
     * @param event 対象のイベントオブジェクト
     */
    fun onExit(@Advice.Argument(1) event: Event) {
        AmbiguousVariableFrames.endFrame(event)
    }
}