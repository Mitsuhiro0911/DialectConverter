package dialect

import org.dom4j.Node
import org.dom4j.io.SAXReader

class Converter {
    private val reader = SAXReader()
    private val document = reader.read("./data/corpas/dialect_data.xml")
    private val cp = ContextProcessor()
    private val convertedText = ArrayList<String>()

    /**
     * 遠州弁変換メソッド群のハブ。形態素解析情報を元に、変換方式を決定する。
     */
    fun convert (parsedDataList: ArrayList<ParseResultData>) {
        for (parsedData in parsedDataList) {
            var convertedFlag = false
            println(parsedData)
            // ルールでの変換が難しい単語を個別処理で変換
            convertedFlag = uniqueConvert(parsedData)
            // 接尾辞の場合、直前の単語の処理で纏めて解析しているため、処理をスキップ
            if (parsedData.lexicaCategoryClassification1 == "接尾" && parsedData.lexicaCategoryClassification2 == "人名") {
                continue
            } else if (parsedData.lexicaCategoryClassification1 == "副詞化") {
                cp.doAdverbization(parsedData, convertedText)
            }

            // 上記までで遠州弁に変換されなかった単語は品詞別に変換処理
            if (!convertedFlag) {
                if (parsedData.lexicaCategory == "副詞" || parsedData.lexicaCategoryClassification2 == "副詞可能") {
                    convertedFlag = convertAdverb(parsedData)
                } else if (parsedData.lexicaCategory == "名詞") {
                    convertedFlag = convertNoun(parsedDataList, parsedData)
                } else if (parsedData.lexicaCategory == "形容詞") {
                    convertedFlag = convertAdjective(parsedData)
                } else if(parsedData.lexicaCategory == "助詞") {
                    convertedFlag = convertParticle(parsedData)
                }
            }

            // 上記までで遠州弁に変換されなかった単語はそのまま出力
            if (!convertedFlag) {
                convertedText.add(parsedData.surface)
            }
        }
        for (output in convertedText) {
            print(output)
        }
    }

    /**
     * 名詞を遠州弁に変換する。
     */
    private fun convertNoun (parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean{
        var convertedFlag = false
        // lexicaCategoryが名詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> = document.selectNodes("//standard[../lexicaCategory[text()='名詞']][../importance[text()='3']]")
        // 接頭辞の結合処理
        cp.appendPrefix(parsedDataList, parsedData, convertedText)
        // inputTextの末尾の場合は接尾辞がつく可能性はないため接尾辞処理はしない
        if (parsedDataList.indexOf(parsedData) + 1 != parsedDataList.size) {
            // 直後が人名の接尾辞の場合
            if (parsedDataList[parsedDataList.indexOf(parsedData) + 1].lexicaCategoryClassification1 == "接尾" && parsedDataList[parsedDataList.indexOf(
                    parsedData
                ) + 1].lexicaCategoryClassification2 == "人名"
            ) {
                // 接尾辞の結合処理
                cp.appnedSuffix(parsedDataList, parsedData)
            }
        }
        convertedFlag = simplConvert(parsedData, standardWordList)
        return convertedFlag
    }

    /**
     * 形容詞を遠州弁に変換する。
     */
    private fun convertAdjective (parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが形容詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> = document.selectNodes("//standard[../lexicaCategory[text()='形容詞']][../importance[text()='3']]")
        convertedFlag = simplConvert(parsedData, standardWordList)
        return convertedFlag
    }

    /**
     * 副詞を遠州弁に変換する。
     */
    private fun convertAdverb (parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが副詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> = document.selectNodes("//standard[../lexicaCategory[text()='副詞']][../importance[text()='3']]")
        convertedFlag = simplConvert(parsedData, standardWordList)
        return convertedFlag
    }

    /**
     * 助詞を遠州弁に変換する。
     */
    private fun convertParticle(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが副詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> = document.selectNodes("//standard[../lexicaCategory[text()='助詞']][../importance[text()='3']]")
        // 助詞がくっつく直前の単語を抽出
        val preWordList: List<Node> = document.selectNodes("//pre_word[../lexicaCategory[text()='助詞']][../importance[text()='3']]")
        // 助詞がくっつく直前の単語が正しい時のみ遠州弁に変換する
        for (preword in preWordList) {
            if (preword.text == convertedText[convertedText.size - 1]) {
                convertedText.removeAt(convertedText.size - 1)
                convertedFlag = simplConvert(parsedData, standardWordList)
            }
        }

        return convertedFlag
    }

    /**
     * ルール化が難しい単語の個別変換処理
     */
    fun uniqueConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if(parsedData.surface == "ごと" && parsedData.lexicaCategoryClassification1 == "接尾") {
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='ごと']]")
            convertedText.add(ensyuWord[0].text)
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 各変換メソッドから呼ばれる共通変換処理。
     */
    private fun simplConvert (parsedData: ParseResultData, standardWordList: List<Node>): Boolean {
        var convertedFlag = false
        for (standardWord in standardWordList) {
            if (standardWord.text == parsedData.surface) {
                // 標準語に対応した遠州弁を取得
                val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='${standardWord.text}']]")
                convertedText.add(ensyuWord[0].text)
                convertedFlag = true
            }
        }
        return convertedFlag
    }
}