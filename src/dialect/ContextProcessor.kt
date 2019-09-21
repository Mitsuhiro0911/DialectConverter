package dialect

class ContextProcessor {
    /**
     * parsedDataの前後のデータを取得する。
     */
    fun getContextData(cd: ConverterData, parsedDataList: ArrayList<ParseResultData>) {
        // parsedDataが末尾のデータでなければ、次データの情報を取得し、cd.parsedNextDataへ格納
        cd.parsedNextData = null
        if (cd.index + 1 < parsedDataList.size) {
            cd.parsedNextData = parsedDataList[cd.index + 1]
        }

        // cd.parsedNextDataが末尾のデータでなければ、次データの情報を取得し、cd.parsedNextNextDataへ格納
        cd.parsedNextNextData = null
        if (cd.index + 2 < parsedDataList.size) {
            cd.parsedNextNextData = parsedDataList[cd.index + 2]
        }

        // parsedDataが先頭のデータでなければ、前データの情報を取得し、cd.parsedBeforeDataへ格納
        cd.parsedBeforeData = null
        if (cd.index - 1 > -1) {
            cd.parsedBeforeData = parsedDataList[cd.index - 1]
        }

        // cd.parsedBeforeDataが先頭のデータでなければ、前データの情報を取得し、cd.parsedBeforeBeforeDataへ格納
        cd.parsedBeforeBeforeData = null
        if (cd.index - 2 > -1) {
            cd.parsedBeforeBeforeData = parsedDataList[cd.index - 2]
        }

        // cd.parsedBeforeBeforeDataが先頭のデータでなければ、前データの情報を取得し、cd.parsed3BeforeDataへ格納
        cd.parsed3BeforeData = null
        if (cd.index - 3 > -1) {
            cd.parsed3BeforeData = parsedDataList[cd.index - 3]
        }
    }

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
            if (parsedDataList[parsedDataList.indexOf(parsedData) - 1].lexicaCate == "接頭詞") {
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
        parsedData.lexicaCate = "副詞"
        // convertedTextの末尾の要素を除外(重複排除)
        convertedText.removeAt(convertedText.size - 1)
    }
}