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
    // parsedDataListの参照中データの前のデータ
    private var parsedBeforeData: ParseResultData? = null
    // parsedDataListの参照中データの前の前のデータ
    private var parsedBeforeBeforeData: ParseResultData? = null
    // parsedDataListの参照中データの3つ前のデータ
    private var parsed3BeforeData: ParseResultData? = null
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
        var i = 0
        for (parsedData in parsedDataList) {
            // スキップフラグが1(変換不要)の場合処理をスキップ
            if (skipFlagList!![parsedDataList.indexOf(parsedData)] == 1) {
                i = i.plus(1)
                continue
            }

            // parsedDataが末尾のデータでなければ、次データの情報を取得し、parsedNextDataへ格納
            parsedNextData = null
            if (i + 1 < parsedDataList.size) {
                parsedNextData = parsedDataList[i + 1]
            }

            // parsedNextDataが末尾のデータでなければ、次データの情報を取得し、parsedNextNextDataへ格納
            parsedNextNextData = null
            if (i + 2 < parsedDataList.size) {
                parsedNextNextData = parsedDataList[i + 2]
            }

            // parsedDataが先頭のデータでなければ、前データの情報を取得し、parsedBeforeDataへ格納
            parsedBeforeData = null
            if (i - 1 > -1) {
                parsedBeforeData = parsedDataList[i - 1]
            }

            // parsedBeforeDataが先頭のデータでなければ、前データの情報を取得し、parsedBeforeBeforeDataへ格納
            parsedBeforeBeforeData = null
            if (i - 2 > -1) {
                parsedBeforeBeforeData = parsedDataList[i - 2]
            }

            // parsedBeforeBeforeDataが先頭のデータでなければ、前データの情報を取得し、parsed3BeforeDataへ格納
            parsed3BeforeData = null
            if (i - 3 > -1) {
                parsed3BeforeData = parsedDataList[i - 3]
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
                } else if (parsedData.lexicaCategory == "動詞") {
                    convertedFlag = convertVerb(parsedData)
                }
            }

            // 上記までで遠州弁に変換されなかった単語はそのまま出力
            if (!convertedFlag) {
                convertedText.add(parsedData.surface)
            }
            i = i.plus(1)
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
     * 動詞を遠州弁に変換する。
     */
    private fun convertVerb(parsedData: ParseResultData): Boolean {
        // TODO:遠州弁コーパスの未然ウ接続、未然ヌ接続、未然レル接続、連用タ接続の情報がまだ作成中のため、記載すること
        var convertedFlag = false
        // lexicaCategoryが動詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='動詞']][../importance[text()='3']]")
        for (standardWord in standardWordList) {
            // 動詞は原型の情報で比較する
            if (standardWord.text == parsedData.originalPattern) {
                convertedFlag = getVerbConjugational(parsedData, standardWord.text)
            }
        }
        return convertedFlag
    }

    /**
     * 活用形を考慮して遠州弁の動詞を取得する。
     */
    private fun getVerbConjugational(parsedData: ParseResultData, standardWord: String): Boolean {
        var convertedFlag = false
        // 標準語に対応した遠州弁を取得
        var ensyuWord: List<Node>? = null
        if (parsedData.conjugationalType == "基本形") {
            ensyuWord = document.selectNodes("//conjugational/kihon[../../standard[text()='${standardWord}']]")
        } else if (parsedData.conjugationalType == "未然形" || parsedData.conjugationalType == "未然ウ接続" || parsedData.conjugationalType == "未然ヌ接続" || parsedData.conjugationalType == "未然レル接続") {
            // 未然形への変換
            ensyuWord = document.selectNodes("//conjugational/mizen[../../standard[text()='${standardWord}']]")
            if (parsedNextData != null) {
                if (parsedNextData!!.surface == "う" && parsedNextData!!.lexicaCategory == "助動詞") {
                    // 未然ウ接続への変換
                    val mizen_u: List<Node> = document.selectNodes("//conjugational/mizen_u[../../standard[text()='${standardWord}']]")
                    if (mizen_u[0].text != "") {
                        ensyuWord = mizen_u
                    }
                } else if (parsedNextData!!.surface == "ぬ" && parsedNextData!!.lexicaCategory == "助動詞") {
                    // 未然ヌ接続への変換
                    val mizen_nu: List<Node> = document.selectNodes("//conjugational/mizen_nu[../../standard[text()='${standardWord}']]")
                    if (mizen_nu[0].text != "") {
                        ensyuWord = mizen_nu
                    }
                } else if (parsedNextData!!.surface == "れる" && parsedNextData!!.lexicaCategory == "動詞" && parsedNextData!!.lexicaCategoryClassification1 == "接尾") {
                    // 未然レル接続への変換
                    val mizen_reru: List<Node> = document.selectNodes("//conjugational/mizen_reru[../../standard[text()='${standardWord}']]")
                    if (mizen_reru[0].text != "") {
                        ensyuWord = mizen_reru
                    }
                }
            }
        } else if (parsedData.conjugationalType == "連用形" || parsedData.conjugationalType == "連用タ接続") {
            ensyuWord = document.selectNodes("//conjugational/renyo[../../standard[text()='${standardWord}']]")
            // 直後が助動詞の「た」で、変換先の遠州弁が連用タ接続を取りうる場合、連用タ接続の遠州弁に変換する
            if (parsedNextData != null) {
                // TODO:「挟んだ」のように濁音が続く場合の連用タ接続をどのように処理するか考慮する
                if ((parsedNextData!!.surface == "た" || parsedNextData!!.surface == "だ") && parsedNextData!!.lexicaCategory == "助動詞") {
                    val renyo_ta: List<Node> = document.selectNodes("//conjugational/renyo_ta[../../standard[text()='${standardWord}']]")
                    // 遠州弁コーパスの連用タ接続の情報が空文字でなければ、連用タ接続を取りうる遠州弁と判定できる
                    if (renyo_ta[0].text != "") {
                        ensyuWord = renyo_ta
                    }
                }
            }

        } else if (parsedData.conjugationalType == "仮定形") {
            ensyuWord = document.selectNodes("//conjugational/katei[../../standard[text()='${standardWord}']]")
        } else if (parsedData.conjugationalType == "命令ｅ" || parsedData.conjugationalType == "命令ｒｏ" || parsedData.conjugationalType == "命令ｙｏ" || parsedData.conjugationalType == "命令ｉ") {
            ensyuWord = document.selectNodes("//conjugational/meirei[../../standard[text()='${standardWord}']]")
        }
        // TODO:今後必要に応じて実装
//                else if (parsedData.conjugationalType == "文語基本形") {}
//                else if (parsedData.conjugationalType == "未然特殊") {}
//                else if (parsedData.conjugationalType == "体言接続") {}
//                else if (parsedData.conjugationalType == "体言接続特殊") {}
//                else if (parsedData.conjugationalType == "体言接続特殊２") {}
//                else if (parsedData.conjugationalType == "仮定縮約１") {}
        if (ensyuWord != null) {
            convertedText.add(ensyuWord[0].text)
            convertedFlag = true
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
            // 「だよ、だぞ、ですよ、ですぞ」→「だに」
            convertedFlag = daniConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「だろ、でしょ、だよね」→「だら」
            convertedFlag = daraConvert(parsedDataList, parsedData)
        }
        if (!convertedFlag) {
            // 「ね」→「やぁ」
            convertedFlag = yaConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「した」→「いた」、「しちゃう」→「いちゃう」
//            convertedFlag = itaConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「から、ので、だから、なので」→「だもんで」
            convertedFlag = damondeConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「散髪する、髪の毛を切る、髪を切る、髪切る」→「頭切る」
            convertedFlag = atamaKiruConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「鍵をかける、鍵かける」→「かう」の変換処理
            convertedFlag = kagiwoKauConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「炭酸が抜ける、気が抜ける」→「かが抜ける」の変換処理
            convertedFlag = kagaNukeruConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「内出血する、青あざができる、青あざを作る」→「血が死ぬ」の変換処理
            convertedFlag = chigaShinuConvert(parsedDataList, parsedData)
        }
        if (!convertedFlag) {
            // 「挟む」→「はさげる」の変換処理
            convertedFlag = hasageruConvert(parsedDataList, parsedData)
        }
        if (!convertedFlag) {
            // 「(壊れる、)使えなくなる、使えんくなる」→「ばかになる」の変換処理
            convertedFlag = bakaniNaruConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「不愉快な、不愉快だ、いやだ、いやな」→「いやったい」の変換処理
            convertedFlag = iyattaiConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「水っぽい」→「しゃびしゃび」の変換処理
            convertedFlag = shabishabiConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「仕方がない、仕方ない、しょうがない」→「しょんない」の変換処理
            convertedFlag = syonnai(parsedData)
        }
        return convertedFlag
    }

    /**
     * 「ごと」→「さら」の変換処理
     */
    private fun saraConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "ごと" && parsedData.lexicaCategoryClassification1 == "接尾") {
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='ごと']]")
            convertedText.add(ensyuWord[0].text)
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「だよ、だぞ、ですよ、ですぞ」→「だに」の変換処理
     */
    private fun daniConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if ((parsedData.surface == "よ" && parsedData.lexicaCategory == "助詞") || (parsedData.surface == "ぞ" && parsedData.lexicaCategory == "助詞")) {
            // 助詞がくっつく直前の単語を抽出
            val preWordList: List<Node> = document.selectNodes("//pre_word[../enshu[text()='だに']]")
            if (parsedBeforeData != null) {
                for (preWord in preWordList) {
                    if (preWord.text == parsedBeforeData!!.surface) {
                        convertedText.add("だに")
                        convertedFlag = true
                    }
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「だろ、でしょ、だよね」→「だら」の変換処理
     */
    private fun daraConvert(parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        // 使用中のneologd辞書だと「○○だろう」「○○でしょう」が謝解析されるが、その他辞書なら問題なし
        var convertedFlag = false
        var daraFlag = false
        // 「だろ、でしょ」の変換判定
        if ((parsedData.surface == "だろ" && parsedData.lexicaCategory == "助動詞") || (parsedData.surface == "でしょ" && parsedData.lexicaCategory == "助動詞")) {
            daraFlag = true
        }
        // 「だよね」の変換判定
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
     * 「ね」→「やぁ」の変換処理
     */
    private fun yaConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // 直前の単語が形容詞であることが必要
        if (parsedBeforeData != null) {
            if ((parsedData.surface == "ね" && parsedData.lexicaCategory == "助詞") && (parsedBeforeData!!.lexicaCategory == "形容詞")) {
                convertedText.add("やぁ")
                convertedFlag = true
            }
        }
        return convertedFlag
    }

    /**
     * 「した」→「いた」、「しちゃう」→「いちゃう」の変換処理
     */
    private fun itaConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // 直前の単語が動詞で末尾の文字が「し」の場合変換する
        if (parsedBeforeData != null) {
            if ((parsedData.surface == "た" && parsedData.lexicaCategory == "助動詞" || (parsedData.originalPattern == "ちゃう" && parsedData.lexicaCategory == "動詞")) && (parsedBeforeData!!.lexicaCategory == "動詞" && parsedBeforeData!!.surface.get(
                    parsedBeforeData!!.surface.length - 1
                ) == 'し')
            ) {
                // 直前の動詞の末尾の「し」を削除
                convertedText[convertedText.size - 1] =
                    "${parsedBeforeData!!.surface.substring(0, parsedBeforeData!!.surface.length - 1)}い"
                convertedText.add("${parsedData.surface}")
                convertedFlag = true
            }
        }
        return convertedFlag
    }

    /**
     * 「から、ので、だから、なので」→「だもんで」の変換処理
     */
    private fun damondeConvert(parsedData: ParseResultData): Boolean {
        // 「ため」は接続助詞ではなく名詞と形態素解析されてしまうため、変換対象から除外
        var convertedFlag = false
        if ((parsedData.surface == "から" || parsedData.surface == "ので") && parsedData.lexicaCategoryClassification1 == "接続助詞") {
            // 「なので」→「なもんで」と変換されるのを防ぐ
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "な" && parsedBeforeData!!.lexicaCategory == "助動詞") {
                    convertedText[convertedText.size - 1] = "だ"
                }
            }
            convertedText.add("もんで")
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「散髪する、髪の毛を切る、髪を切る、髪切る」→「頭切る」の変換処理
     */
    private fun atamaKiruConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // TODO:「散髪した」が「散髪いた」になってしまうバグが発生。(「した」→「いた」が適用されてしまう)
        if (parsedData.originalPattern == "する" && parsedData.lexicaCategory == "動詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "散髪") {
                    // 「散髪する」→「頭切る」
                    convertedText.removeAt(convertedText.size - 1)
                    convertedFlag = getVerbConjugational(parsedData, "散髪する")
                }
            }
        } else if (parsedData.originalPattern == "切る" && parsedData.lexicaCategory == "動詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "髪" || parsedBeforeData!!.surface == "髪の毛") {
                    // 「髪切る、髪の毛切る」→「頭切る」
                    convertedText.removeAt(convertedText.size - 1)
                    convertedFlag = getVerbConjugational(parsedData, "髪を切る")
                } else if (parsedBeforeData!!.surface == "を" && parsedBeforeData!!.lexicaCategory == "助詞") {
                    // 「髪を切る、髪の毛を切る」→「頭切る」
                    if (parsedBeforeBeforeData != null) {
                        if (parsedBeforeBeforeData!!.surface == "髪" || parsedBeforeBeforeData!!.surface == "髪の毛") {
                            convertedText.removeAt(convertedText.size - 1)
                            convertedText.removeAt(convertedText.size - 1)
                            convertedFlag = getVerbConjugational(parsedData, "髪を切る")
                        }
                    }
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「鍵をかける、鍵かける」→「かう」の変換処理
     */
    private fun kagiwoKauConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.originalPattern == "かける" && parsedData.lexicaCategory == "動詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "鍵") {
                    convertedFlag = getVerbConjugational(parsedData, "鍵かける")
                } else if (parsedBeforeData!!.surface == "を" && parsedBeforeData!!.lexicaCategory == "助詞") {
                    if (parsedBeforeBeforeData != null) {
                        if (parsedBeforeBeforeData!!.surface == "鍵") {
                            convertedFlag = getVerbConjugational(parsedData, "鍵をかける")
                        }
                    }
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「炭酸が抜ける、気が抜ける」→「かが抜ける」の変換処理
     */
    private fun kagaNukeruConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.originalPattern == "抜ける" && parsedData.lexicaCategory == "動詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "が" && parsedBeforeData!!.lexicaCategory == "助詞") {
                    if (parsedBeforeBeforeData != null) {
                        if (parsedBeforeBeforeData!!.surface == "炭酸" || parsedBeforeBeforeData!!.surface == "気") {
                            convertedText.removeAt(convertedText.size - 1)
                            convertedText.removeAt(convertedText.size - 1)
                            convertedFlag = getVerbConjugational(parsedData, "炭酸が抜ける")
                        }
                    }
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「内出血する、青あざができる、青あざを作る」→「血が死ぬ」の変換処理
     */
    private fun chigaShinuConvert(parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.originalPattern == "する" && parsedData.lexicaCategory == "動詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "内出血") {
                    convertedText.removeAt(convertedText.size - 1)
                    convertedFlag = getVerbConjugational(parsedData, "内出血する")
                } else if (parsedBeforeData!!.surface == "を" && parsedBeforeData!!.lexicaCategory == "助詞") {
                    if (parsedBeforeBeforeData != null) {
                        if (parsedBeforeBeforeData!!.surface == "内出血") {
                            convertedText.removeAt(convertedText.size - 1)
                            convertedText.removeAt(convertedText.size - 1)
                            convertedFlag = getVerbConjugational(parsedData, "内出血する")
                        }
                    }
                }
            }
        } else if (parsedData.originalPattern == "できる" && parsedData.lexicaCategory == "動詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "が" && parsedBeforeData!!.lexicaCategory == "助詞") {
                    if (parsedBeforeBeforeData != null) {
                        if (parsedBeforeBeforeData!!.surface == "あざ") {
                            if (parsed3BeforeData != null) {
                                if (parsed3BeforeData!!.surface == "青") {
                                    convertedText.removeAt(convertedText.size - 1)
                                    convertedText.removeAt(convertedText.size - 1)
                                    convertedText.removeAt(convertedText.size - 1)
                                    convertedFlag = getVerbConjugational(parsedData, "青あざができる")
                                }
                            }
                        }
                    }
                }
            }
        } else if (parsedData.originalPattern == "作る" && parsedData.lexicaCategory == "動詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "を" && parsedBeforeData!!.lexicaCategory == "助詞") {
                    if (parsedBeforeBeforeData != null) {
                        if (parsedBeforeBeforeData!!.surface == "あざ") {
                            if (parsed3BeforeData != null) {
                                if (parsed3BeforeData!!.surface == "青") {
                                    convertedText.removeAt(convertedText.size - 1)
                                    convertedText.removeAt(convertedText.size - 1)
                                    convertedText.removeAt(convertedText.size - 1)
                                    convertedFlag = getVerbConjugational(parsedData, "青あざを作る")
                                }
                            }
                        }
                    }
                }
            }
        }
        // 連用タ接続に変換する際、血が死ん「た」になるのを防ぐ処理
        if (convertedFlag) {
            if (convertedText[convertedText.size - 1] == "血が死ん") {
                skipFlagList!![(parsedDataList.indexOf(parsedNextData!!))] = 1
                convertedText.add("だ")
            }
        }
        return convertedFlag
    }

    /**
     * 「挟む」→「はさげる」の変換処理
     */
    private fun hasageruConvert(parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.originalPattern == "挟む" && parsedData.lexicaCategory == "動詞") {
            convertedFlag = getVerbConjugational(parsedData, "挟む")
        }
        // 連用タ接続に変換する際、はさげ「だ」になるのを防ぐ処理
        if (convertedFlag) {
            if (parsedData.conjugationalType == "連用タ接続") {
                skipFlagList!![(parsedDataList.indexOf(parsedNextData!!))] = 1
                convertedText.add("た")
            }
        }
        return convertedFlag
    }

    /**
     * 「(壊れる、)使えなくなる、使えんくなる」→「ばかになる」の変換処理(「壊れる」は通常の動詞変換メソッドでOK)
     */
    private fun bakaniNaruConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.originalPattern == "なる" && parsedData.lexicaCategory == "動詞") {
            if (parsedBeforeData != null) {
                if ((parsedBeforeData!!.surface == "なく" && parsedBeforeData!!.lexicaCategory == "助動詞") ||
                    (parsedBeforeData!!.surface == "く" && parsedBeforeData!!.lexicaCategory == "動詞")) {
                    if (parsedBeforeBeforeData != null) {
                        if (parsedBeforeBeforeData!!.originalPattern == "使える") {
                            convertedText.removeAt(convertedText.size - 1)
                            convertedText.removeAt(convertedText.size - 1)
                            convertedFlag = getVerbConjugational(parsedData, "使えなくなる")
                        }
                    }
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「不愉快な、不愉快だ、いやだ、いやな」→「いやったい」の変換処理
     */
    private fun iyattaiConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if ((parsedData.surface == "な" || parsedData.surface == "だ") && parsedData.lexicaCategory == "助動詞") {
            if (parsedBeforeData != null) {
                if ((parsedBeforeData!!.surface == "いや" || parsedBeforeData!!.surface == "不愉快") && parsedBeforeData!!.lexicaCategory == "名詞") {
                    val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='不愉快な']]")
                    convertedText.removeAt(convertedText.size - 1)
                    convertedText.add(ensyuWord[0].text)
                    convertedFlag = true
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「水っぽい」→「しゃびしゃび」の変換処理
     */
    private fun shabishabiConvert(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "水っぽい") {
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='水っぽい']]")
            // 「水っぽい」は形容詞、「しゃびしゃび」は名詞なので"な"を末尾に付加。「しゃびしゃび」をすばり表現する標準語が見当たらない
            convertedText.add("${ensyuWord[0].text}な")
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「仕方がない、仕方ない、しょうがない」→「しょんない」の変換処理
     */
    private fun syonnai(parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "仕方がない" && parsedData.lexicaCategory == "名詞") {
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='仕方がない']]")
            convertedText.add("${ensyuWord[0].text}")
            convertedFlag = true
        } else if (parsedData.surface == "ない" && parsedData.lexicaCategory == "助動詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "仕方" && parsedBeforeData!!.lexicaCategory == "名詞") {
                    val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='仕方ない']]")
                    convertedText.removeAt(convertedText.size - 1)
                    convertedText.add("${ensyuWord[0].text}")
                    convertedFlag = true
                }
            }
        } else if (parsedData.surface == "ない" && parsedData.lexicaCategory == "形容詞") {
            if (parsedBeforeData != null) {
                if (parsedBeforeData!!.surface == "しょうが" && parsedBeforeData!!.lexicaCategory == "名詞") {
                    val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='しょうがない']]")
                    convertedText.removeAt(convertedText.size - 1)
                    convertedText.add("${ensyuWord[0].text}")
                    convertedFlag = true
                }
            }
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