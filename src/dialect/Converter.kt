package dialect

import org.dom4j.Node
import org.dom4j.io.SAXReader

class Converter {
    private val reader = SAXReader()
    private val document = reader.read("./data/corpas/dialect_data.xml")

    fun convert (parsedDataList: ArrayList<ParseResultData>) {
        // lexicaCategoryが品詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> = document.selectNodes("//standard[../lexicaCategory[text()='名詞']][../importance[text()='3']]")
        for (standardWord in standardWordList) {
            println(standardWord.text)
        }
        for (parsedData in parsedDataList) {
            if (parsedData.lexicaCategory == "名詞") {
            }
        }

    }
}