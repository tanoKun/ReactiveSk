# ReactiveSk
Skript に **オブジェクト指向・軽いリアクティブ**を使用したプログラミングを可能にします。

# 目次
<!-- TOC -->
* [ReactiveSk](#reactivesk)
* [目次](#目次)
* [クラスの定義](#クラスの定義)
  * [フィールド](#フィールド)
* [クラス関数の定義](#クラス関数の定義)
  * [戻り値](#戻り値)
  * [呼び出し](#呼び出し)
    * [非中断 - non suspend](#非中断---non-suspend)
    * [中断 - suspend](#中断---suspend)
    * [実行 - execute](#実行---execute)
  * [引数・パラメーター](#引数パラメーター)
    * [配列型](#配列型)
* [クラスのインスタンス化](#クラスのインスタンス化)
<!-- TOC -->

# クラスの定義
`class` キーワードを使用して定義されます。

```
class TestClass:
```

クラスは[フィールドセクション](#フィールド)、[クラス関数](#クラス関数の定義) で構成されます。
[クラス関数](#クラス関数の定義) は必ずしも必須ではありません。

## フィールド

`field` セクションの中に定義されます。
`val | var 名前: 型` で定義され、[関数の引数](#引数パラメーター) と同じです。
- `val` はイミュータブルを表します。
- `var` はミュータブルを表します。

```
class TestClass:
    field:
        var name: PersonName
        val longs: array of long
```

# クラス関数の定義
`function` キーワードを使用して定義されます。また、skript本来の関数とは違い
[クラス](#クラスの定義) の内部にのみ定義できる関数です。
```
function sum(a: long, b: long):: long:
    fun return a + b
```

## 戻り値
skriptの関数では、戻り値を指定する際 `return` キーワードで定義します。
しかし、クラス関数 では `fun return` キーワードを使用し、定義します。
```
fun return a + b
fun return {_returnValue}
```

## 呼び出し

クラス関数の呼び出し方についてです。
それぞれの例として以下を参照します。
```
function sum(a: long, b: long):: long:
    wait 10 seconds
    fun return a + b
```

### 非中断 - non suspend
クラス関数の返り値を待ちません。
つまり、クラス関数が途中で中断される場合、正しい返り値を取得できません。
ただし、クラス関数の実行は最後まで行われます。

```
set {_value} to {object}.sum(1, 10)
send "%{_value}%" # <none>
send "%{object}.sum(1, 10)%" # <none>
```

### 中断 - suspend
クラス関数の返り値を待ち、呼び出し元を中断します。
これは `expression` としての定義ではなく、`effect` であることに注意してください。
また、チェーンの様なつなぎ方はできません。
```
await {object}.sum(1, 10) on {_value}
send "%{_value}%" # 11

await {object}.sum(1, 10) # suspend

await {object}.sum(1, 10).functionName() on {_value} # can't
```

### 実行 - execute
クラス関数の実行だけを行い中断しません。

```
{object}.sum(1, 10) # non suspend
send "aaaaaa" # immediately execute
```

## 引数・パラメーター
クラス関数の引数は `変数名: 型` を使用して定義します。
各パラメータは明示的に型を指定する必要があります。

```
function sum(a: long, b: long):
  //do anything
```

### 配列型
引数をリスト・配列としたい場合 `array of` キーワードを使用し、定義します。
```
function sum(a: array of long, b: long):
  //do anything
```

# クラスのインスタンス化
構文として `create クラス名 with 引数` でインスタンス化できます。
これは `expression` として定義されます。

```
set {person} to create Person with {_name}, {_age}, {_job}
```