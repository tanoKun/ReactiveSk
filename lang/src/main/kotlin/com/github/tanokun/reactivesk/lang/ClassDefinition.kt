package com.github.tanokun.reactivesk.lang

/**
 * クラス定義の構文的な構造を表現するデータです。
 *
 * @property className クラスの名前
 * @property constructor コンストラクタ定義
 * @property uninitializedProperty クラス本体で `field` キーワードを用いて宣言されたプロパティのリスト
 *                                 初期化が保証されていないため、[Property.UninitializedProperty] となります。
 * @property functions クラス内で定義された関数のリスト
 */
data class ClassDefinition(
    val className: Identifier,
    val constructor: Constructor,
    val uninitializedProperty: List<Property.UninitializedProperty>,
    val functions: List<Function>
) {
    /**
     * クラスのプロパティ
     * プロパティの状態 (初期化が保証されているかどうか) によって型が分かれます。
     */
    sealed interface Property {
        val propertyName: Identifier
        val typeName: Identifier
        val isArray: Boolean
        val modifiers: PropertyModifier

        /**
         * コンストラクタによって初期化が保証されたプロパティ
         *
         * @see Constructor.Parameter.AsInitializedProperty
         */
        sealed interface InitializedProperty : Property

        /**
         * クラス本体で `field` キーワードを用いて宣言されたプロパティ
         * 初期化が保証されていないため、[Property.UninitializedProperty] として扱われます。
         */
        data class UninitializedProperty(
            override val propertyName: Identifier,
            override val typeName: Identifier,
            override val isArray: Boolean,
            override val modifiers: PropertyModifier
        ) : Property
    }

    /**
     * 関数やコンストラクタがスローする可能性のあるエラーを表現します。
     * @property error スローされるエラーの型名
     */
    data class Throws(val error: Identifier)

    /**
     * クラスのコンストラクタ
     * @property parameters コンストラクタの引数リスト
     * @property throws このコンストラクタがスローする可能性のあるエラーのリスト
     */
    data class Constructor(val parameters: List<Parameter>, val throws: List<Throws>) {
        /**
         * コンストラクタの引数
         * 引数が単なるパラメータか、プロパティ宣言を兼ねるかによって型が分かれます。
         */
        sealed interface Parameter {
            val parameterName: Identifier
            val typeName: Identifier
            val isArray: Boolean

            /**
             * プロパティ宣言を兼ねない、純粋なコンストラクタ引数
             */
            data class NotProperty(override val parameterName: Identifier, override val typeName: Identifier, override val isArray: Boolean) : Parameter

            /**
             * コンストラクタの引数であり、同時にクラスのプロパティ宣言も兼ねるパラメータ
             * Kotlinの `class User(val name: String)` における `val name` に相当します。
             *
             * この方法で宣言されたプロパティは、コンストラクタ呼び出し時に必ず値が渡されるため、
             * 定義上、初期化が保証されているとみなされます。
             */
            data class AsInitializedProperty(
                override val parameterName: Identifier,
                override val typeName: Identifier,
                override val isArray: Boolean,
                override val modifiers: PropertyModifier
            ) : Parameter, Property.InitializedProperty {
                override val propertyName: Identifier = parameterName
            }
        }
    }

    /**
     * クラス関数です。
     *
     * @property functionName 関数名
     * @property returns 関数の戻り値
     * @property parameters 関数の引数リスト
     * @property throws この関数がスローする可能性のあるエラーのリスト
     */
    data class Function(val functionName: Identifier, val modifiers: PropertyModifier, val returns: Returns, val parameters: List<Parameter>, val throws: List<Throws>) {
        data class Parameter(val parameterName: Identifier, val typeName: Identifier, val isArray: Boolean)
        data class Returns(val typeName: Identifier, val isArray: Boolean)
    }

    /**
     * このクラスが持つ全てのプロパティのリストを返します。
     *
     * 初期化が保証されていないプロパティと、
     * コンストラクタによって初期化が保証されたプロパティの両方を含みます。
     *
     * @return クラスの全プロパティのリスト
     */
    fun getProperties(): List<Property> {
        return uninitializedProperty + constructor.parameters.filterIsInstance<Constructor.Parameter.AsInitializedProperty>()
    }
}