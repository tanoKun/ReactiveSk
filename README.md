# ReactiveSk
Skript に **オブジェクト指向・軽いリアクティブ**を使用したプログラミングを可能にします。

<!-- TOC -->
* [ReactiveSk](#reactivesk)
* [クラス定義](#クラス定義)
  * [コンストラクタとプロパティ](#コンストラクタとプロパティ)
  * [field セクション](#field-セクション)
  * [修飾子](#修飾子)
    * [アクセス修飾子](#アクセス修飾子)
    * [宣言修飾子](#宣言修飾子)
  * [init セクション](#init-セクション)
* [クラス関数定義](#クラス関数定義)
* [クラス関数呼び出し](#クラス関数呼び出し)
  * [Non Suspend (Expression, Effect)](#non-suspend-expression-effect)
* [型と配列](#型と配列)
* [変数宣言（型制限変数）](#変数宣言型制限変数)
  * [変数アクセス](#変数アクセス)
* [監視 - Observer](#監視---observer)
  * [クラス監視](#クラス監視)
    * [参照可能な変数](#参照可能な変数)
  * [通知 - Notify](#通知---notify)
* [例](#例)
  * [カウンタークラス](#カウンタークラス)
<!-- TOC -->

# クラス定義
- `class Name[constructorParameters...]:` でクラスを定義します。
- クラス本体には、`field:`、`init:`、`function ...:` を記述できます。

```
class Test2[test2: array of string, val test: string]:
    field:
        private var test3: string

    init:
        resolve test3 := "resolved!"
        
    function name(test: string):: string:
        fun return "aaa"
```

## コンストラクタとプロパティ
- 角括弧 `[...]` 内がコンストラクタ引数です。
- そのうち `val` / `var` / `factor` が付いた引数は `フィールド` として自動生成され、インスタンス生成時に自動代入されます。
  - 例: `val test: string` → `test` は読み取り専用プロパティ
  - 例: `var test: string` → `test` は再代入可能プロパティ
- `val` / `var` が付いていない引数はプロパティにはなりませんが、`init` セクション内からパラメータ名（例: `[test2]` のような参照表記）でアクセスできます。

## field セクション
- 追加の `フィールド` を宣言します。
- アクセス修飾子や可変性を指定できます。

`private var test3: string`

`field` で宣言したフィールドは、`init` セクションで必ず `resolve` により初期化（解決）しないとエラーになります。

`resolve test3 := "resolved!"`

また、解決する必要のあるフィールドは 直接 `[name]` でアクセスすることができません。
行いたい場合は、`[this].name` を経由しアクセスしてください。

## 修飾子
修飾子には `アクセス修飾子`　と `宣言修飾子` の2種類があります。

### アクセス修飾子
- `pravate` : クラス外からアクセス不可

### 宣言修飾子
- `val` : 読み取り専用プロパティ
- `var` : 再代入可能プロパティ
- `factor` : 読み取り専用プロパティ + 監視の要因 (リアクティブ)

`factor` は `var` と同様に再代入が可能です。
この修飾子が付いている変数が変更されると、それを自動的に通知します。

## init セクション
コンストラクタ実行後に呼ばれる初期化ブロックです。
- `field` で宣言した全フィールドを `resolve` で初期化する
- `val` / `var` が付いていないコンストラクタ引数にアクセスして初期化ロジックを記述する（例: `[test2]` 参照）

自分自身のインスタンスを指す変数 `[this]` が使用可能です。

```
init:
    resolve test3 := "resolved!"
```

全フィールドを全ての経路で解決する必要があります。

# クラス関数定義
`function name(parameters...):: returnType:` の形で宣言します。
関数本体で値を返すには `fun return <expression>` を使用します。

returnType は省略が可能です。

また、自分自身のインスタンスを指す変数 `[this]` が使用可能です。

```
function name(test: string):: string:
    fun return "aaa"
    
function name(test: string):
    # code...
```

関数に返り値が設定されている場合、
全ての経路で `fun return` が**実行されることが保証されます**。


# クラス関数呼び出し

## Non Suspend (Expression, Effect)
> Syntax: call `%functionName%` in `%object%`

```
val count := call count in [classInstance]

call count in [classInstance]
```
この関数の呼び出しは、`中断されず`に**即座に値を返します。**
関数内に `wait` につながる構文がある場合、値が `null` になる可能性があります。

- ポイント
  - パラメータには型を明示します（配列も可）。
# 型と配列
- 基本型の指定: `string` など
- 配列型の指定: `array of <type>`
  - 例: `array of string`

# 変数宣言（型制限変数）
- 変数は `val` (再代入不可) と `var`(再代入可) で宣言します。
- 構文
  - 明示的な型と初期化: `val | var 名 (型) := 値`
  - 型推論で初期化: `val | var 名 := 値`
- 例
```
val title (string) := "test"   # 型明示 + 初期化
val msg := "hello"             # 型推論 + 初期化

var count (integer) := 0        # 可変
[count] <- [count] + 1          # 再代入

val count (integer)             # 宣言
[count] := 10                   # 代入
```

`val name` のように、初期化も型指定もない宣言はできません。
また、変数宣言のみを行う場合、**生存範囲を大きく**することは推奨しません。

## 変数アクセス
基本的に変数は `[name]` でアクセスができます。
変数の型が自作型である場合 `[name].field` で、フィールドにアクセスが可能です。

`クラス関数` や `init セクション` において定義されている`引数`や`プロパティ`は、
全て上記の方法でアクセス可能です。

# 監視 - Observer
`factor` 修飾子が付いたフィールドが変更されると、その変更が通知されます。
パターンで言う `オブザーバーパターン` を実装します。

## クラス監視

```
class Player[factor level: long, factor exp: long]:
    function addExp(amount: long):
        notify [this].exp <- [this].exp + [amount]
        if [this].exp >= 100:
            [this].level <- [this].level + 1
            [this].exp <- [this].exp - 100

observe Player factor level:
    if [instance].level >= 10:
        send "レベル10に到達しました！ おめでとう！"
```

`observe <ClassName> factor <fieldName>:` で監視を開始します。

### 参照可能な変数
- `[instance]` : 変更があったインスタンス
- `[old]` : 変更前の値
- `[new]` : 変更後の値

> 一つのロジックで2回 `factor` 変数を変更した場合、2回通知されます。
> 通知自体はメインスレッドで行われますが、**順序は確定しません**。
> また、全て `suspend` で実行されます。
> 

## 通知 - Notify
フィールド変更時、デフォルトでは通知は行われません。(`[object].field <- value` での変更)

通知を行うには `notify` を使用します。
`notify [this].field <- value` の様にすることで、通知を行うことができます。
この設定は、不要な通知を避けるために実装されています。

# 例
## カウンタークラス
`クラス関数`で実装した`カウンター` と `Skript 本来の関数`で実装した`カウンター` の比較です。

```
class Counter[var count: long]:
    function increment():
        [this].count <- [this].count + 1
        
#200万回分 カウントする。 (Class)
command /class:
    trigger:
        var test2 := create Counter with 0

        loop 20 times:
            set {_start} to now

            loop 100000 times:
                call increment in [test2]
            set {_end} to now
            set {_diff} to difference between {_end} and {_start}
            add {_diff} to {_time} 
            broadcast "処理時間: %{_diff}%"
        
        send "200万回の合計: %{_time}%"
        
#200万回分 カウントする。 (Class)
command /origin:
    trigger:
        loop 20 times:
            set {_start} to now
            loop 100000 times:
                set {_aa} to increment({_aa})
            set {_end} to now
            set {_diff} to difference between {_end} and {_start}
            add {_diff} to {_time} 
            broadcast "処理時間: %{_diff}%"

        send "200万回の合計: %{_time}%"

function increment(count: number) :: number:
    return {_count} + 1
```