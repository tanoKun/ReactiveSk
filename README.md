# ReactiveSk
ReactiveSk enables object-oriented and lightweight reactive programming for Skript.

## Supported Versions
- `Skript 2.6.3 ~ 1.12.2`
- `Paper 1.12.2 ~ 1.21.8`

<!-- TOC -->
* [ReactiveSk](#reactivesk)
  * [Supported Versions](#supported-versions)
* [Class Definition](#class-definition)
  * [Constructor and Properties](#constructor-and-properties)
  * [field section](#field-section)
  * [Modifiers](#modifiers)
    * [Access Modifiers](#access-modifiers)
    * [Declaration Modifiers](#declaration-modifiers)
  * [init section](#init-section)
* [Class Function Definition](#class-function-definition)
* [Calling Class Functions](#calling-class-functions)
  * [Non-Suspend (Expression, Effect)](#non-suspend-expression-effect)
* [Types and Arrays](#types-and-arrays)
* [Variable Declaration (Typed Variables)](#variable-declaration-typed-variables)
  * [Variable Access](#variable-access)
* [Observation - Observer](#observation---observer)
  * [Class Observation](#class-observation)
    * [Available Variables](#available-variables)
  * [Notification - Notify](#notification---notify)
* [Grammar Summary](#grammar-summary)
* [Example: Full Class Sample](#example-full-class-sample)
<!-- TOC -->

# Class Definition
- Define a class using `class Name[constructorParameters...]:`.
- The class body can contain `field:`, `init:`, and `function ...:` sections.

```
class Test2[test2: array of string, val test: string]:
    field:
        private var test3: string

    init:
        resolve test3 := "resolved!"
        
    function name(test: string) :: string:
        return "aaa"
        
class Counter[val count: integer]:
    function increment(): Counter:
        send "%[this].count%" to console

        if count of [this] is 12:
            send "it is 12!" to console

        return new Counter([this] -> count + 1)
        
```

## Constructor and Properties
- Constructor parameters are placed inside square brackets `[...]`.
- Parameters marked with `val` / `var` / `factor` become fields automatically and are assigned during instance creation.
  - Example: `val test: string` → `test` is a read-only property
  - Example: `var test: string` → `test` is a mutable property
- Parameters without `val` / `var` do not become properties, but they can be accessed inside the `init` section using the parameter name (e.g. `[test2]`).

## field section
- Declare additional fields in the `field` section.
- You can specify access modifiers and mutability.

`private var test3: string`

Fields declared in `field` must be initialized (resolved) in the `init` section using `resolve`, otherwise an error occurs.

`resolve test3 := "resolved!"`

Fields that must be resolved cannot be accessed directly using `[name]` before they are resolved. If you need to access them, use `[this] -> name`.

## Modifiers
There are two kinds of modifiers: access modifiers and declaration modifiers.

### Access Modifiers
- `private`: Not accessible from outside the class

### Declaration Modifiers
- `val`: Read-only property
- `var`: Mutable property
- `factor`: Read-only property that acts as a reactive factor (observable)

`factor` can be reassigned like `var`. When a variable marked with `factor` changes, it automatically emits notifications.

## init section
The `init` section is an initialization block executed after the constructor.
- Initialize all fields declared in `field` with `resolve`.
- Use constructor parameters without `val` / `var` inside `init` to perform initialization logic (e.g. `[test2]`).

The instance itself is available as `[this]` inside `init`.

```
init:
    resolve test3 := "resolved!"
```

All fields must be resolved on every execution path.

# Class Function Definition
Declare functions as `function name(parameters...) :: returnType:`. Use `return <expression>` to return values from the function body.

The return type is optional.

The instance is available as `[this]` inside functions.

```
function name(test: string) :: string:
    return "aaa"
    
function name(test: string):
    # code...
```

When a return type is declared, all code paths must guarantee execution of a `return`.

# Calling Class Functions

## Non-Suspend (Expression, Effect)
> `%object% -> functionName(%objects%) then functionName(%objects%)...`

```
val count := [classInstance] -> count() then sendCount()

[classInstance] -> count() then sendCount()
```

These calls are non-suspending and return a value immediately. If the function contains constructs that lead to `wait`, the returned value may be `null`.

- Note:
  - Explicit types should be provided for parameters (arrays are supported).

# Types and Arrays
- Basic type example: `string`
- Array type: `array of <type>`
  - Example: `array of string`

# Variable Declaration (Typed Variables)
- Variables are declared with `val` (immutable) or `var` (mutable).
- Syntax:
  - With explicit type and initializer: `val | var name (type) := value`, `[declare] immutable | mutable name (type) := value`
  - With type inference: `val | var name := value`, `[declare] immutable | mutable name := value`
- Examples:
```
val title (string) := "test"         # explicit type + initializer
val msg := "hello"                   # type inference + initializer
immutable title (string) := "test"   # explicit type + initializer
immutable msg := "hello"             # type inference + initializer

var count (integer) := 0             # mutable
set [count] to [count] + 1           # reassignment
mutable count := 0         # mutable
set [count] to [count] + 1           # reassignment

val count (integer)                  # declaration only
set [count] to 10                    # assignment
```

You cannot declare `val name` without a type or initializer. Avoid unnecessarily widening the scope of variables declared without initialization.

## Variable Access
Access variables using `[name]`.
For custom types, access fields with `[name].field`, `[name] -> field`, or `field of [name]`.

Arguments and properties defined in class functions and `init` are accessible using the methods above.

```
send "%[player].level%"
send "%[player]->level%"
send "%[player] -> level%"
send "%level of [player]%"
```

# Observation - Observer
Fields marked with `factor` emit notifications when changed, implementing an observer pattern.

## Class Observation

```
class Player[factor level: long, factor exp: long]:
    function addExp(amount: long):
        notify that set [this] -> exp to [this] -> exp + [amount]
        if [this].exp >= 100:
            notify that set [this].level to [this].level + 1
            set exp of [this] to exp of [this] - 100

observe Player factor level:
    if [instance] -> level >= 10:
        send "Reached level 10! Congratulations!"
```

Start observing with `observe <ClassName> factor <fieldName>:`.

### Available Variables
- `[instance]`: the instance that changed
- `[old]`: previous value
- `[new]`: new value

> If a single logic path changes a `factor` field twice, two notifications are emitted. Notifications run on the main thread, but their ordering is not guaranteed. All observers execute as `suspend`.

## Notification - Notify
By default, changing a field (e.g. `set [object].field to value`) does not emit a notification.
Use `notify that` to emit a notification explicitly. For example:

`notify that set [this].field to value`

This prevents unnecessary notifications by default.

# Grammar Summary
- Short BNF-like summary and usage:

```
class <name>[<constructor-params>]:
    field:
    init:
    function <name>(<params...>) :: <returnType>:
    
set [<name>] to <expression>
set <field-access> to <expression>
notify that set <field-access> to <expression>

observe <class-name> factor <field-name>:
    <statements...>
```

# Example: Full Class Sample
```
class Player[factor level: long, val name: string]:
    field:
        private var metadata: string

    init:
        resolve metadata := "default"

    function addExp(amount: long):
        notify that set [this] -> level to [this] -> level + [amount]
        if [this] -> level >= 100:
            notify that set [this] -> level to level of [this] - 100

observe Player factor level:
    send "%[instance] -> name% leveled up to %[new]!"
```