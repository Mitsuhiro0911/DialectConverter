package dialect

class ContextProcessor {
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

    fun appnedSuffix(
        parsedDataList: ArrayList<ParseResultData>,
        parsedData: ParseResultData
    ) {
        // 直後が接尾辞の場合
        // inputTextの末尾の場合は接尾辞がつく可能性はないため接尾辞処理はしない
        if (parsedDataList.indexOf(parsedData) + 1 != parsedDataList.size) {
            if (parsedDataList[parsedDataList.indexOf(parsedData) + 1].lexicaCategoryClassification1 == "接尾") {
                // 名詞に接尾辞を結合
                parsedData.surface =
                    "${parsedData.surface}${parsedDataList[parsedDataList.indexOf(parsedData) + 1].surface}"
            }
        }
    }
}