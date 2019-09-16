package dialect

data class ParseResultData(
    // 表層系
    var surface: String,
    // 品詞
    var lexicaCate: String,
    // 品詞細分類1
    val lexicaCateClass1: String,
    // 品詞細分類2
    val lexicaCateClass2: String,
    // 品詞細分類3
    val lexicaCateClass3: String,
    // 活用形
    val conjForm: String,
    // 活用型
    val conjType: String,
    // 原形
    val original: String,
    // 読み
    val reading: String,
    // 発音
    val pronunciation: String) {

    data class Builder(
        var surface: String,
        var lexicaCate: String,
        val lexicaCateClass1: String,
        val lexicaCateClass2: String,
        val lexicaCateClass3: String,
        val conjForm: String,
        val conjType: String,
        val original: String,
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
                lexicaCate,
                lexicaCateClass1,
                lexicaCateClass2,
                lexicaCateClass3,
                conjForm,
                conjType,
                original,
                reading,
                pronunciation
            )
        }
    }
}