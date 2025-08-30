package com.github.tanokun.addon.definition.variable

import ch.njol.skript.config.Node
import ch.njol.skript.config.SectionNode

private val Node.shouldTerminateTraversal: Boolean
    get() = key?.startsWith("init") == true || key?.startsWith("function") == true

/**
 * 最上位のノードを取得します。
 *
 * このメソッドは現在のノードから親をたどり、以下の条件のいずれかに該当するまで遡ります：
 * - 親の親がnullになる (configに当たる)
 * - キーが`init`で始まる
 * - キーが`function`で始まる
 *
 * @return 最上位のノード
 */
fun Node.getTopNode(): Node {
    var current: Node = this
    while (current.parent?.parent != null) {
        if (current.shouldTerminateTraversal) break
        current.parent?.let { current = it }
    }

    return current
}

/**
 * 現在のノードからルートノードまでの深さ(スコープ数)を計算します。
 *
 * このメソッドは現在のノードから親をたどり、ルートに到達するまでの
 * 階層数をカウントします。
 *
 * @return ルートノードまでのスコープ数
 */
fun Node.getDepth(): Int {
    var current: Node = this
    var depth = 0

    val correction = if (this is SectionNode) 0 else 1

    while (current.parent != null) {
        if (current.shouldTerminateTraversal) break
        current.parent?.let {
            depth++
            current = it
        }
    }

    return depth - correction
}
