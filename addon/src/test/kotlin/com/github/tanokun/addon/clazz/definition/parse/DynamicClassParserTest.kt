package com.github.tanokun.addon.clazz.definition.parse

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.module.ModuleManager
import com.github.tanokun.addon.runtime.skript.init.mediator.RuntimeConstructorMediator
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import org.bukkit.event.Event
import org.junit.jupiter.api.Assertions.assertNotNull
import java.io.File
import kotlin.test.Test


class DynamicClassParserTest {
    @Test
    fun `ModuleManager should load dependent classes and allow instantiation`() {
        val scriptDir = File("test-scripts-module-manager")
        // クリーンな状態でテストを開始
        if (scriptDir.exists()) scriptDir.deleteRecursively()
        scriptDir.mkdirs()

        try {
            // --- 1. テスト環境のセットアップ ---
            // 以前のテストファイルと同じ内容を準備
            File(scriptDir, "Character.sk").writeText("""
class Test[factor a: string, factor b: long, factor c: string, var d: array of boolean]:
    function change2(a: string, b: long, c: string, d: boolean):
        call change in [this] with "aaa2", 20, "bbb2", false, "aaa"

    private function change(a: string, b: long, c: string, d: boolean, e: string):
        [this].a <- [a]
        [this].b <- [b]
        [this].c <- [c]
        [this].d <- [d]
        send "%[e]%" to console
            """.trimIndent())

            File(scriptDir, "Player.sk").writeText("""
class Person[val name: PersonName, val age: PersonAge, val job: PersonJob]:
class PersonName[val name: string]:    
class PersonAge[val age: long]:
class PersonJob[val jobName: string]:

class Counter[var count: long]:
    function increment():
        [this].count <- [this].count + 1

class Test2[var count: Test]:
            """.trimIndent())

            // --- 2. ModuleManagerの設定と初期化 ---

            // 以前の resolveWellKnownType のロジックをラムダ式 (ClassResolver) として定義
            val classResolver: (Identifier) -> Class<*>? = { typeName ->
                when (typeName.identifier.lowercase()) {
                    "string" -> String::class.java
                    "long" -> Long::class.javaObjectType
                    "int" -> Int::class.javaObjectType
                    "boolean" -> Boolean::class.javaObjectType
                    "void" -> Void.TYPE
                    else -> null
                }
            }

            // ModuleManagerをインスタンス化
            val moduleManager = ModuleManager(scriptDir, classResolver)

            // ★★★ メインの処理 ★★★
            // この一文で、全ファイルの解析、依存関係グラフ構築、循環参照チェック、
            // バイトコード生成、そしてファイル単位のクラスローダーでのロードがすべて実行される。
            moduleManager.initialize()

            // --- 3. 検証 ---

            println("--- Verifying loaded class and triggering instance creation ---")

            // ModuleManagerからロード済みのクラスを取得
            val counterClass = moduleManager.getLoadedClass(Identifier("PersonAge")) ?: return
            val counterClass1 = moduleManager.getLoadedClass(Identifier("PersonName")) ?: return
            val counterClass2 = moduleManager.getLoadedClass(Identifier("PersonJob")) ?: return

            val counterClass3 = moduleManager.getLoadedClass(Identifier("Person")) ?: return


            // クラスが正しくロードされたことをアサート
            assertNotNull(counterClass, "Counter class should be loaded by the ModuleManager")

            // 型安全な方法でコンストラクタを取得 (引数は Mediator と long)
            // DynamicClassのサブクラスであると仮定
            val constructor = counterClass.getConstructor(RuntimeConstructorMediator::class.java, Long::class.javaObjectType)

            val test: Event = RuntimeConstructorMediator() // Event型であると仮定

            val counterInstance = constructor.newInstance(test, 10L)

            assertNotNull(counterInstance, "Instance of Counter should be created successfully")
            println("Successfully instantiated: $counterInstance")

            // 以前のテストと同様のランタイム呼び出し
            AmbiguousVariableFrames.beginFrame(test, 1)
            AmbiguousVariableFrames.set(test, 0, "aaaa")

        } finally {
            // --- 4. クリーンアップ ---
            // テストが成功しても失敗しても、必ずテスト用ディレクトリを削除する
            scriptDir.deleteRecursively()
        }
    }
}

