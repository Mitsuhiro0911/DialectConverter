package dialect

data class ParseResultData(
    // 表層系
    var surface: String,
    // 品詞
    val lexicaCategory: String,
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
    val pronunciation: String
)