package dialect

import org.dom4j.Node
import org.dom4j.io.SAXReader

class Converter {
    private val reader = SAXReader()
    private val document = reader.read("./data/corpas/dialect_data.xml")

    fun convert (parsedDataList: ArrayList<ParseResultData>) {
        // lexicaCategoryが品詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> = document.selectNodes("//standard[../lexicaCategory[text()='名詞']][../importance[text()='3']]")
        for (parsedData in parsedDataList) {
            var convertedFlag = false
            // 名詞の変換
            if (parsedData.lexicaCategory == "名詞") {
                for (standardWord in standardWordList) {
                    if (standardWord.text == parsedData.surface) {
                        // 標準語に対応した遠州弁を取得
                        val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='${standardWord.text}']]")
                        print(ensyuWord[0].text)
                        convertedFlag = true
                    }
                }
            }

            // 遠州弁に変換されなかった単語はそのまま出力
            if (!convertedFlag) {
                print(parsedData.surface)
            }
        }
    }
}