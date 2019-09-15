package dialect

class ContextProcessor {
    /**
     * 接頭辞の結合処理を行う。
     */
    fun appendPrefix(
        parsedDataList: ArrayList<ParseResultData>,
        parsedData: ParseResultData,
        convertedText: ArrayList<String>
    ) {
        // inputTextの先頭の場合は接頭辞がつく可能性はないため接頭辞処理はしない
        if (parsedDataList.indexOf(parsedData) - 1 != -1) {
            // 直前が接頭辞の場合
            if (parsedDataList[parsedDataList.indexOf(parsedData) - 1].lexicaCategory == "接頭詞") {
                // 名詞に接頭辞を結合
                parsedData.surface = "${convertedText[convertedText.size - 1]}${parsedData.surface}"
                // convertedTextの末尾の要素(接頭辞)を除外(重複排除)
                convertedText.removeAt(convertedText.size - 1)
            }
        }
    }

    /**
     * 接尾辞の結合処理を行う。
     */
    fun appnedSuffix(
        parsedDataList: ArrayList<ParseResultData>,
        parsedData: ParseResultData,
        index: Int
    ) {
        // 名詞に接尾辞を結合
        parsedData.surface = "${parsedData.surface}${parsedDataList[index + 1].surface}"
    }

    /**
     * 品詞細分類1が副詞化の時呼ばれる。直前の単語と結合し、品詞が副詞に変わる。
     */
    fun doAdverbization(parsedData: ParseResultData, convertedText: ArrayList<String>) {
        // 直前の単語と結合し、副詞を作成
        parsedData.surface = "${convertedText[convertedText.size - 1]}${parsedData.surface}"
        parsedData.lexicaCategory = "副詞"
        // convertedTextの末尾の要素を除外(重複排除)
        convertedText.removeAt(convertedText.size - 1)
    }
}