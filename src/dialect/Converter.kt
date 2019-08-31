package dialect

import org.dom4j.Node
import org.dom4j.io.SAXReader

class Converter {
    private val reader = SAXReader()
    private val document = reader.read("./data/corpas/dialect_data.xml")
    private val cp = ContextProcessor()
    // 遠州弁変換後のテキストデータ
    private val convertedText = ArrayList<String>()
    // parsedDataListの参照中データの次のデータ
    private var parsedNextData: ParseResultData? = null
    // parsedDataListの参照中データの次の次のデータ
    private var parsedNextNextData: ParseResultData? = null
    // 変換処理の要・不要を判定するフラグ。parsedDataListの要素とインデックスが対応付いている。
    private var skipFlagList: ArrayList<Int>? = null

    /**
     * 遠州弁変換メソッド群のハブ。形態素解析情報を元に、変換方式を決定する。
     */
    fun convert(parsedDataList: ArrayList<ParseResultData>) {
        // スキップフラグを0(変換必要)で初期化
        skipFlagList = arrayListOf()
        for (i in 0 until parsedDataList.size) {
            skipFlagList!!.add(0)
        }
        for (parsedData in parsedDataList) {
            // スキップフラグが1(変換不要)の場合処理をスキップ
            if (skipFlagList!![parsedDataList.indexOf(parsedData)] == 1) {
                continue
            }

            // parsedDataが末尾のデータでなければ、次データの情報を取得し、parsedNextDataへ格納
            parsedNextData = null
            if (parsedDataList.indexOf(parsedData) + 1 != parsedDataList.size) {
                parsedNextData = parsedDataList[parsedDataList.indexOf(parsedData) + 1]
            }

            // parsedNextDataが末尾のデータでなければ、次データの情報を取得し、parsedNextNextDataへ格納
            parsedNextNextData = null
            if (parsedDataList.indexOf(parsedNextData) + 1 != parsedDataList.size) {
                parsedNextNextData = parsedDataList[parsedDataList.indexOf(parsedNextData) + 1]
            }

            var convertedFlag = false
            println(parsedData)
            // ルールでの変換が難しい単語を個別処理で変換
            convertedFlag = uniqueConvert(parsedDataList, parsedData)
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
                } else if (parsedData.lexicaCategory == "助詞") {
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
    private fun convertNoun(parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが名詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='名詞']][../importance[text()='3']]")
        // 接頭辞の結合処理
        cp.appendPrefix(parsedDataList, parsedData, convertedText)
        // inputTextの末尾の場合は接尾辞処理はしない
        if (parsedNextData != null) {
            // 直後が人名の接尾辞の場合
            if (parsedNextData!!.lexicaCategoryClassification1 == "接尾" && parsedNextData!!.lexicaCategoryClassification2 == "人名"
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
    private fun convertAdjective(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが形容詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='形容詞']][../importance[text()='3']]")
        convertedFlag = simplConvert(parsedData, standardWordList)
        return convertedFlag
    }

    /**
     * 副詞を遠州弁に変換する。
     */
    private fun convertAdverb(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが副詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='副詞']][../importance[text()='3']]")
        convertedFlag = simplConvert(parsedData, standardWordList)
        return convertedFlag
    }

    /**
     * 助詞を遠州弁に変換する。
     */
    private fun convertParticle(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが副詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='助詞']][../importance[text()='3']]")
        // 助詞がくっつく直前の単語を抽出
        val preWordList: List<Node> =
            document.selectNodes("//pre_word[../lexicaCategory[text()='助詞']][../importance[text()='3']]")
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
    fun uniqueConvert(parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (!convertedFlag) {
            // 「ごと」→「さら」
            convertedFlag = saraConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「だろ、でしょ、だよね」→「だら」
            convertedFlag = daraConvert(parsedDataList, parsedData)
        }
        return convertedFlag
    }

    /**
     * 「ごと」→「さら」の変換処理
     */
    private  fun saraConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "ごと" && parsedData.lexicaCategoryClassification1 == "接尾") {
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='ごと']]")
            convertedText.add(ensyuWord[0].text)
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「だろ、でしょ、だよね」→「だら」の変換処理
     */
    private  fun daraConvert(parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        // 使用中のneologd辞書だと「○○だろう」「○○でしょう」が謝解析されるが、その他辞書なら問題なし
        var convertedFlag = false
        var daraFlag = false
        // 「だろ、でしょ」の変換判定
        if ((parsedData.surface == "だろ" && parsedData.lexicaCategory == "助動詞") || (parsedData.surface == "でしょ" && parsedData.lexicaCategory == "助動詞")) {
            daraFlag = true
        }
        // 「だろう」の変換判定
        if (parsedNextData != null && parsedNextNextData != null) {
            if ((parsedData.surface == "だ" && parsedData.lexicaCategory == "助動詞") && (parsedNextData!!.surface == "よ" && parsedNextData!!.lexicaCategory == "助詞") && (parsedNextNextData!!.surface == "ね" && parsedNextNextData!!.lexicaCategory == "助詞")) {
                daraFlag = true
                // 「よ」「ね」を結合してから変換するため、それらの解析は不要となる。よってスキップフラグを立てる
                skipFlagList!![(parsedDataList.indexOf(parsedNextData!!))] = 1
                skipFlagList!![(parsedDataList.indexOf(parsedNextNextData!!))] = 1
            }
        }
        if (daraFlag) {
            convertedText.add("だら")
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 各変換メソッドから呼ばれる共通変換処理。
     */
    private fun simplConvert(parsedData: ParseResultData, standardWordList: List<Node>): Boolean {
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