package dialect

data class ParseResultData(
    // インナークラスからアウタークラスに代入する(builderメソッド)際にvarをvalに変更できるため、保守性が向上できる。
    // 表層系
    var surface: String,
    // 品詞
    var lexicaCategory: String,
    // 品詞細分類1
    val lexicaCategoryClassification1: String,
    // 品詞細分類2
    val lexicaCategoryClassification2: String,
    // 品詞細分類3
    val lexicaCategoryClassification3: String,
    // 活用形
    val conjugationalForm: String,
    // 活用型
    val conjugationalType: String,
    // 原形
    val originalPattern: String,
    // 読み
    val reading: String,
    // 発音
    val pronunciation: String) {

    data class Builder(
        var surface: String,
        var lexicaCategory: String,
        val lexicaCategoryClassification1: String,
        val lexicaCategoryClassification2: String,
        val lexicaCategoryClassification3: String,
        val conjugationalForm: String,
        val conjugationalType: String,
        val originalPattern: String,
        var reading: String = "*",
        var pronunciation: String = "*") {

        /**
         * 「読み」の情報がある場合セットする
         */
        fun reading(parseResult: Array<String>) = apply {
            if (parseResult.size > 8) {
                reading = parseResult[8]
            }
        }

        /**
         * 「発音」の情報がある場合セットする
         */
        fun pronunciation(parseResult: Array<String>) = apply {
            if (parseResult.size > 9) {
                reading = parseResult[9]
            }
        }

        /**
         * ビルダーの実行
         */
        fun builder(): ParseResultData {
            return ParseResultData(
                surface,
                lexicaCategory,
                lexicaCategoryClassification1,
                lexicaCategoryClassification2,
                lexicaCategoryClassification3,
                conjugationalForm,
                conjugationalType,
                originalPattern,
                reading,
                pronunciation
            )
        }
    }
}