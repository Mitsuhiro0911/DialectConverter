package dialect

import org.dom4j.Node
import org.dom4j.io.SAXReader

class Converter {
    private val reader = SAXReader()
    private val document = reader.read("./data/corpas/dialect_data.xml")
    private val cp = ContextProcessor()

    fun convert (parsedDataList: ArrayList<ParseResultData>) {
        val convertedText = ArrayList<String>()
        // lexicaCategoryが名詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> = document.selectNodes("//standard[../lexicaCategory[text()='名詞']][../importance[text()='3']]")
        for (parsedData in parsedDataList) {
            var convertedFlag = false
            // 名詞の変換
            if (parsedData.lexicaCategory == "名詞") {
                // inputTextの先頭が名詞の場合は接頭辞がつく可能性はないため接頭辞処理はしない
                if (parsedDataList.indexOf(parsedData) - 1 != -1) {
                    // 直前が接頭辞の場合
                    if (parsedDataList[parsedDataList.indexOf(parsedData) - 1].lexicaCategory == "接頭詞") {
                        // 名詞に接頭辞を結合
                        parsedData.surface = "${convertedText[convertedText.size - 1]}${parsedData.surface}"
                        // convertedTextの末尾の要素(接頭辞)を除外
                        convertedText.removeAt(convertedText.size - 1)

                    }
                }

                // 直後が接尾辞の場合
                // inputTextの末尾が名詞の場合は接尾辞がつく可能性はないため接尾辞処理はしない
                if (parsedDataList.indexOf(parsedData) + 1 != parsedDataList.size) {
                    if (parsedDataList[parsedDataList.indexOf(parsedData) + 1].lexicaCategoryClassification1 == "接尾") {
                    }
                }

                for (standardWord in standardWordList) {
                    if (standardWord.text == parsedData.surface) {
                        // 標準語に対応した遠州弁を取得
                        val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='${standardWord.text}']]")
                        convertedText.add(ensyuWord[0].text)
                        convertedFlag = true
                    }
                }
            }

            // 遠州弁に変換されなかった単語はそのまま出力
            if (!convertedFlag) {
                convertedText.add(parsedData.surface)
            }
        }
        for (output in convertedText) {
            println(output)
        }
    }
}