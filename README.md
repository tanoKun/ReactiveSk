# ReactiveSk
Skript に **オブジェクト指向・軽いリアクティブ**を使用したプログラミングを可能にします。

<!-- TOC -->
* [ReactiveSk](#reactivesk)
* [クラス定義](#クラス定義)
  * [コンストラクタとプロパティ](#コンストラクタとプロパティ)
  * [field セクション](#field-セクション)
  * [init セクション](#init-セクション)
* [クラス関数定義](#クラス関数定義)
* [型と配列](#型と配列)
* [変数宣言（型制限変数）](#変数宣言型制限変数)
  * [変数アクセス](#変数アクセス)
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
- そのうち `val` / `var` が付いた引数は `フィールド` として自動生成され、インスタンス生成時に自動代入されます。
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

## init セクション
コンストラクタ実行後に呼ばれる初期化ブロックです。
- `field` で宣言した全フィールドを `resolve` で初期化する
- `val` / `var` が付いていないコンストラクタ引数にアクセスして初期化ロジックを記述する（例: `[test2]` 参照）

自分自身のインスタンスを指す変数 `[this]` が使用可能です。

```
init:
    resolve test3 := "resolved!"
```


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
[count] -> [count] + 1          # 再代入

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