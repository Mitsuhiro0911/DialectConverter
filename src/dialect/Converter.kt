package dialect

import org.dom4j.Node
import org.dom4j.io.SAXReader

class Converter {
    private val reader = SAXReader()
    private val document = reader.read("./data/corpas/dialect_data.xml")
    private val cp = ContextProcessor()

    /**
     * 遠州弁変換メソッド群のハブ。形態素解析情報を元に、変換方式を決定する。
     */
    fun convert(parsedDataList: ArrayList<ParseResultData>): ArrayList<String> {
        val cd = ConverterData(convertedText = ArrayList())
        // スキップフラグを0(変換必要)で初期化
        cd.skipFlagList = arrayListOf()
        for (i in 0 until parsedDataList.size) {
            cd.skipFlagList!!.add(0)
        }

        for (parsedData in parsedDataList) {
            // スキップフラグが1(変換不要)の場合処理をスキップ
            if (cd.skipFlagList!![cd.index] == 1) {
                cd.index = cd.index.plus(1)
                continue
            }

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

            var convertedFlag = false
            println(parsedData)
            // ルールでの変換が難しい単語を個別処理で変換
            convertedFlag = uniqueConvert(cd, parsedDataList, parsedData)
            // 副詞化
            if (parsedData.lexicaCateClass1 == "副詞化") {
                cp.doAdverbization(parsedData, cd.convertedText)
            }

            // 上記までで遠州弁に変換されなかった単語は品詞別に変換処理
            if (!convertedFlag) {
                if (parsedData.lexicaCate == "副詞" || parsedData.lexicaCateClass2 == "副詞可能") {
                    convertedFlag = convertAdverb(cd, parsedData)
                } else if (parsedData.lexicaCate == "名詞") {
                    convertedFlag = convertNoun(cd, parsedDataList, parsedData)
                } else if (parsedData.lexicaCate == "形容詞") {
                    convertedFlag = convertAdjective(cd, parsedData)
                } else if (parsedData.lexicaCate == "動詞") {
                    convertedFlag = convertVerb(cd, parsedData)
                }
            }

            // 上記までで遠州弁に変換されなかった単語はそのまま出力
            if (!convertedFlag) {
                cd.convertedText.add(parsedData.surface)
            }
            cd.index = cd.index.plus(1)
        }
        for (output in cd.convertedText) {
            print(output)
        }
        return cd.convertedText
    }

    /**
     * 名詞を遠州弁に変換する。
     */
    private fun convertNoun(cd: ConverterData, parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが名詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='名詞']][../importance[text()='3']]")
        // 接頭辞の結合処理
        cp.appendPrefix(parsedDataList, parsedData, cd.convertedText)
        // inputTextの末尾の場合は接尾辞処理はしない
        // 直後が人名の接尾辞の場合
        if (cd.parsedNextData?.lexicaCateClass1 == "接尾" && cd.parsedNextData?.lexicaCateClass2 == "人名"
        ) {
            // 接尾辞の結合処理
            cp.appnedSuffix(parsedDataList, parsedData, cd.index)
            // 接尾辞の場合、直後の単語の処理で纏めて解析しているため、次の処理をスキップ
            cd.skipFlagList!![cd.index + 1] = 1
        }
        convertedFlag = simplConvert(cd, parsedData, standardWordList)
        return convertedFlag
    }

    /**
     * 形容詞を遠州弁に変換する。
     */
    private fun convertAdjective(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが形容詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='形容詞']][../importance[text()='3']]")
        convertedFlag = simplConvert(cd, parsedData, standardWordList)
        return convertedFlag
    }

    /**
     * 副詞を遠州弁に変換する。
     */
    private fun convertAdverb(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // lexicaCategoryが副詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='副詞']][../importance[text()='3']]")
        convertedFlag = simplConvert(cd, parsedData, standardWordList)
        return convertedFlag
    }

    /**
     * 動詞を遠州弁に変換する。
     */
    private fun convertVerb(cd: ConverterData, parsedData: ParseResultData): Boolean {
        // TODO:遠州弁コーパスの未然ウ接続、未然ヌ接続、未然レル接続、連用タ接続の情報がまだ作成中のため、記載すること
        var convertedFlag = false
        // lexicaCategoryが動詞 且つ importanceが3のstandard(標準語)情報を抽出
        val standardWordList: List<Node> =
            document.selectNodes("//standard[../lexicaCategory[text()='動詞']][../importance[text()='3']]")
        for (standardWord in standardWordList) {
            // 動詞は原型の情報で比較する
            if (standardWord.text == parsedData.original) {
                convertedFlag = getVerbConjugational(cd, parsedData, standardWord.text)
            }
        }
        return convertedFlag
    }

    /**
     * 活用形を考慮して遠州弁の動詞を取得する。
     */
    private fun getVerbConjugational(cd: ConverterData, parsedData: ParseResultData, standardWord: String): Boolean {
        var convertedFlag = false
        // 標準語に対応した遠州弁を取得
        var ensyuWord: List<Node>? = null
        if (parsedData.conjType == "基本形") {
            ensyuWord = document.selectNodes("//conjugational/kihon[../../standard[text()='${standardWord}']]")
        } else if (parsedData.conjType == "未然形" || parsedData.conjType == "未然ウ接続" || parsedData.conjType == "未然ヌ接続" || parsedData.conjType == "未然レル接続") {
            // 未然形への変換
            ensyuWord = document.selectNodes("//conjugational/mizen[../../standard[text()='${standardWord}']]")
            if (cd.parsedNextData?.surface == "う" && cd.parsedNextData?.lexicaCate == "助動詞") {
                // 未然ウ接続への変換
                val mizen_u: List<Node> =
                    document.selectNodes("//conjugational/mizen_u[../../standard[text()='${standardWord}']]")
                if (mizen_u[0].text != "") {
                    ensyuWord = mizen_u
                }
            } else if (cd.parsedNextData?.surface == "ぬ" && cd.parsedNextData?.lexicaCate == "助動詞") {
                // 未然ヌ接続への変換
                val mizen_nu: List<Node> =
                    document.selectNodes("//conjugational/mizen_nu[../../standard[text()='${standardWord}']]")
                if (mizen_nu[0].text != "") {
                    ensyuWord = mizen_nu
                }
            } else if (cd.parsedNextData?.surface == "れる" && cd.parsedNextData?.lexicaCate == "動詞" && cd.parsedNextData?.lexicaCateClass1 == "接尾") {
                // 未然レル接続への変換
                val mizen_reru: List<Node> =
                    document.selectNodes("//conjugational/mizen_reru[../../standard[text()='${standardWord}']]")
                if (mizen_reru[0].text != "") {
                    ensyuWord = mizen_reru
                }
            }

        } else if (parsedData.conjType == "連用形" || parsedData.conjType == "連用タ接続") {
            ensyuWord = document.selectNodes("//conjugational/renyo[../../standard[text()='${standardWord}']]")
            // 直後が助動詞の「た」で、変換先の遠州弁が連用タ接続を取りうる場合、連用タ接続の遠州弁に変換する
            // TODO:「挟んだ」のように濁音が続く場合の連用タ接続をどのように処理するか考慮する
            if ((cd.parsedNextData?.surface == "た" || cd.parsedNextData?.surface == "だ") && cd.parsedNextData?.lexicaCate == "助動詞") {
                val renyo_ta: List<Node> =
                    document.selectNodes("//conjugational/renyo_ta[../../standard[text()='${standardWord}']]")
                // 遠州弁コーパスの連用タ接続の情報が空文字でなければ、連用タ接続を取りうる遠州弁と判定できる
                if (renyo_ta[0].text != "") {
                    ensyuWord = renyo_ta
                }
            }

        } else if (parsedData.conjType == "仮定形") {
            ensyuWord = document.selectNodes("//conjugational/katei[../../standard[text()='${standardWord}']]")
        } else if (parsedData.conjType == "命令ｅ" || parsedData.conjType == "命令ｒｏ" || parsedData.conjType == "命令ｙｏ" || parsedData.conjType == "命令ｉ") {
            ensyuWord = document.selectNodes("//conjugational/meirei[../../standard[text()='${standardWord}']]")
        }
        // TODO:今後必要に応じて実装
//                else if (parsedData.conjType == "文語基本形") {}
//                else if (parsedData.conjType == "未然特殊") {}
//                else if (parsedData.conjType == "体言接続") {}
//                else if (parsedData.conjType == "体言接続特殊") {}
//                else if (parsedData.conjType == "体言接続特殊２") {}
//                else if (parsedData.conjType == "仮定縮約１") {}
        if (ensyuWord != null) {
            cd.convertedText.add(ensyuWord[0].text)
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * ルール化が難しい単語の個別変換処理
     */
    fun uniqueConvert(cd: ConverterData, parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (!convertedFlag) {
            // 「ごと」→「さら」
            convertedFlag = saraConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「だよ、だぞ、ですよ、ですぞ」→「だに」
            convertedFlag = daniConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「だろ、だろうね、だろうな、でしょ、でしょう、でしょうね、でしょうな、だよね、ですよね」→「だら」の変換処理
            convertedFlag = daraConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「ね」→「やぁ」
            convertedFlag = yaConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「した」→「いた」、「しちゃう」→「いちゃう」
//            convertedFlag = itaConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「から、ので、だから、なので」→「だもんで」
            convertedFlag = damondeConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「散髪する、髪の毛を切る、髪を切る、髪切る」→「頭切る」
            convertedFlag = atamaKiruConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「鍵をかける、鍵かける」→「かう」の変換処理
            convertedFlag = kagiwoKauConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「炭酸が抜ける、気が抜ける」→「かが抜ける」の変換処理
            convertedFlag = kagaNukeruConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「内出血する、青あざができる、青あざを作る」→「血が死ぬ」の変換処理
            convertedFlag = chigaShinuConvert(cd, parsedDataList, parsedData)
        }
        if (!convertedFlag) {
            // 「挟む」→「はさげる」の変換処理
            convertedFlag = hasageruConvert(cd, parsedDataList, parsedData)
        }
        if (!convertedFlag) {
            // 「(壊れる、)使えなくなる、使えんくなる」→「ばかになる」の変換処理
            convertedFlag = bakaniNaruConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「不愉快な、不愉快だ、いやだ、いやな」→「いやったい」の変換処理
            convertedFlag = iyattaiConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「水っぽい」→「しゃびしゃび」の変換処理
            convertedFlag = shabishabiConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「仕方がない、仕方ない、しょうがない」→「しょんない」の変換処理
            convertedFlag = syonnaiConvert(cd, parsedData)
        }
        if (!convertedFlag) {
            // 「熱い」→「ちんちん」の変換処理
//            convertedFlag = tintinConvert(parsedData)
        }
        if (!convertedFlag) {
            // 「すぐに、急いで」→「ちゃっちゃと」の変換処理
//            convertedFlag = tyattyatoConvert(parsedDataList, parsedData)
        }
        if (!convertedFlag) {
            // 「なくては、なくちゃ」→「にゃ」の変換処理
            convertedFlag = nyaConvert(cd, parsedData)
        }
        return convertedFlag
    }

    /**
     * 「ごと」→「さら」の変換処理
     */
    private fun saraConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "ごと" && parsedData.lexicaCateClass1 == "接尾") {
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='ごと']]")
            cd.convertedText.add(ensyuWord[0].text)
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「だよ、だぞ、ですよ、ですぞ」→「だに」の変換処理
     */
    private fun daniConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if ((parsedData.surface == "よ" && parsedData.lexicaCate == "助詞") || (parsedData.surface == "ぞ" && parsedData.lexicaCate == "助詞")) {
            // 助詞がくっつく直前の単語を抽出
            val preWordList: List<Node> = document.selectNodes("//pre_word[../enshu[text()='だに']]")
            for (preWord in preWordList) {
                if (preWord.text == cd.parsedBeforeData?.surface) {
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    cd.convertedText.add("だに")
                    convertedFlag = true
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「だろ、だろうね、だろうな、でしょ、でしょう、でしょうね、でしょうな、だよね、ですよね」→「だら」の変換処理
     */
    private fun daraConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        // 使用中のneologd辞書だと「○○だろう」「○○でしょう」が謝解析されるが、その他辞書なら問題なし
        var convertedFlag = false
        var daraFlag = false
        // 「だろ、だろうね、だろうな、でしょ、でしょう、でしょうね、でしょうな」の変換判定
        if ((parsedData.surface == "だろ" && parsedData.lexicaCate == "助動詞") || (parsedData.surface == "でしょ" && parsedData.lexicaCate == "助動詞")) {
            daraFlag = true
            if (cd.parsedNextData?.surface == "う" && cd.parsedNextData?.lexicaCate == "助動詞") {
                if ((cd.parsedNextNextData?.surface == "ね" || cd.parsedNextNextData?.surface == "な") && cd.parsedNextNextData?.lexicaCate == "助詞") {
                    // 「だろうね、だろうな、でしょうね、でしょうな」の場合
                    cd.skipFlagList!![cd.index + 1] = 1
                    cd.skipFlagList!![cd.index + 2] = 1
                } else if (cd.parsedNextNextData?.surface == "か" && cd.parsedNextNextData?.lexicaCate == "助詞") {
                    // 「だろうか、でしょうか」の場合は変換しない
                    daraFlag = false
                }
                cd.skipFlagList!![cd.index + 1] = 1
            }
        }
        // 「だよね、ですよね」の変換判定
        if (((parsedData.surface == "だ" || parsedData.surface == "です") && parsedData.lexicaCate == "助動詞") && (cd.parsedNextData?.surface == "よ" && cd.parsedNextData?.lexicaCate == "助詞") && (cd.parsedNextNextData?.surface == "ね" && cd.parsedNextNextData?.lexicaCate == "助詞")) {
            daraFlag = true
            // 「よ」「ね」を結合してから変換するため、それらの解析は不要となる。よってスキップフラグを立てる
            cd.skipFlagList!![cd.index + 1] = 1
            cd.skipFlagList!![cd.index + 2] = 1
        }
        if (daraFlag) {
            cd.convertedText.add("だら")
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「ね」→「やぁ」の変換処理
     */
    private fun yaConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // 直前の単語が形容詞であることが必要
        if ((parsedData.surface == "ね" && parsedData.lexicaCate == "助詞") && (cd.parsedBeforeData?.lexicaCate == "形容詞")) {
            cd.convertedText.add("やぁ")
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「した」→「いた」、「しちゃう」→「いちゃう」の変換処理
     */
    private fun itaConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // 直前の単語が動詞で末尾の文字が「し」の場合変換する
        if (cd.parsedBeforeData != null) {
            if ((parsedData.surface == "た" && parsedData.lexicaCate == "助動詞" || (parsedData.original == "ちゃう" && parsedData.lexicaCate == "動詞")) && (cd.parsedBeforeData!!.lexicaCate == "動詞" && cd.parsedBeforeData!!.surface.get(
                    cd.parsedBeforeData!!.surface.length - 1
                ) == 'し')
            ) {
                // 直前の動詞の末尾の「し」を削除
                cd.convertedText[cd.convertedText.size - 1] =
                    "${cd.parsedBeforeData!!.surface.substring(0, cd.parsedBeforeData!!.surface.length - 1)}い"
                cd.convertedText.add("${parsedData.surface}")
                convertedFlag = true
            }
        }
        return convertedFlag
    }

    /**
     * 「から、ので、だから、なので」→「だもんで」の変換処理
     */
    private fun damondeConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        // 「ため」は接続助詞ではなく名詞と形態素解析されてしまうため、変換対象から除外
        var convertedFlag = false
        if ((parsedData.surface == "から" || parsedData.surface == "ので") && parsedData.lexicaCateClass1 == "接続助詞") {
            // 「なので」→「なもんで」と変換されるのを防ぐ
            if (cd.parsedBeforeData?.surface == "な" && cd.parsedBeforeData?.lexicaCate == "助動詞") {
                cd.convertedText[cd.convertedText.size - 1] = "だ"
            }
            cd.convertedText.add("もんで")
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「散髪する、髪の毛を切る、髪を切る、髪切る」→「頭切る」の変換処理
     */
    private fun atamaKiruConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        // TODO:「散髪した」が「散髪いた」になってしまうバグが発生。(「した」→「いた」が適用されてしまう)
        if (parsedData.original == "する" && parsedData.lexicaCate == "動詞") {
            if (cd.parsedBeforeData?.surface == "散髪") {
                // 「散髪する」→「頭切る」
                cd.convertedText.removeAt(cd.convertedText.size - 1)
                convertedFlag = getVerbConjugational(cd, parsedData, "散髪する")
            }
        } else if (parsedData.original == "切る" && parsedData.lexicaCate == "動詞") {
            if (cd.parsedBeforeData?.surface == "髪" || cd.parsedBeforeData?.surface == "髪の毛") {
                // 「髪切る、髪の毛切る」→「頭切る」
                cd.convertedText.removeAt(cd.convertedText.size - 1)
                convertedFlag = getVerbConjugational(cd, parsedData, "髪を切る")
            } else if (cd.parsedBeforeData!!.surface == "を" && cd.parsedBeforeData!!.lexicaCate == "助詞") {
                // 「髪を切る、髪の毛を切る」→「頭切る」
                if (cd.parsedBeforeBeforeData?.surface == "髪" || cd.parsedBeforeBeforeData?.surface == "髪の毛") {
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    convertedFlag = getVerbConjugational(cd, parsedData, "髪を切る")
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「鍵をかける、鍵かける」→「かう」の変換処理
     */
    private fun kagiwoKauConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.original == "かける" && parsedData.lexicaCate == "動詞") {
            if (cd.parsedBeforeData?.surface == "鍵") {
                convertedFlag = getVerbConjugational(cd, parsedData, "鍵かける")
            } else if (cd.parsedBeforeData?.surface == "を" && cd.parsedBeforeData?.lexicaCate == "助詞") {
                if (cd.parsedBeforeBeforeData?.surface == "鍵") {
                    convertedFlag = getVerbConjugational(cd, parsedData, "鍵をかける")
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「炭酸が抜ける、気が抜ける」→「かが抜ける」の変換処理
     */
    private fun kagaNukeruConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.original == "抜ける" && parsedData.lexicaCate == "動詞") {
            if (cd.parsedBeforeData?.surface == "が" && cd.parsedBeforeData?.lexicaCate == "助詞") {
                if (cd.parsedBeforeBeforeData?.surface == "炭酸" || cd.parsedBeforeBeforeData?.surface == "気") {
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    convertedFlag = getVerbConjugational(cd, parsedData, "炭酸が抜ける")
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「内出血する、青あざができる、青あざを作る」→「血が死ぬ」の変換処理
     */
    private fun chigaShinuConvert(cd: ConverterData, parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.original == "する" && parsedData.lexicaCate == "動詞") {
            if (cd.parsedBeforeData?.surface == "内出血") {
                cd.convertedText.removeAt(cd.convertedText.size - 1)
                convertedFlag = getVerbConjugational(cd, parsedData, "内出血する")
            } else if (cd.parsedBeforeData?.surface == "を" && cd.parsedBeforeData?.lexicaCate == "助詞") {
                if (cd.parsedBeforeBeforeData?.surface == "内出血") {
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    convertedFlag = getVerbConjugational(cd, parsedData, "内出血する")
                }
            }
        } else if (parsedData.original == "できる" && parsedData.lexicaCate == "動詞") {
            if (cd.parsedBeforeData?.surface == "が" && cd.parsedBeforeData?.lexicaCate == "助詞") {
                if (cd.parsedBeforeBeforeData?.surface == "あざ") {
                    if (cd.parsed3BeforeData?.surface == "青") {
                        cd.convertedText.removeAt(cd.convertedText.size - 1)
                        cd.convertedText.removeAt(cd.convertedText.size - 1)
                        cd.convertedText.removeAt(cd.convertedText.size - 1)
                        convertedFlag = getVerbConjugational(cd, parsedData, "青あざができる")
                    }
                }
            }
        } else if (parsedData.original == "作る" && parsedData.lexicaCate == "動詞") {
            if (cd.parsedBeforeData?.surface == "を" && cd.parsedBeforeData?.lexicaCate == "助詞") {
                if (cd.parsedBeforeBeforeData?.surface == "あざ") {
                    if (cd.parsed3BeforeData?.surface == "青") {
                        cd.convertedText.removeAt(cd.convertedText.size - 1)
                        cd.convertedText.removeAt(cd.convertedText.size - 1)
                        cd.convertedText.removeAt(cd.convertedText.size - 1)
                        convertedFlag = getVerbConjugational(cd, parsedData, "青あざを作る")
                    }
                }
            }
        }
        // 連用タ接続に変換する際、血が死ん「た」になるのを防ぐ処理
        if (convertedFlag) {
            if (cd.convertedText[cd.convertedText.size - 1] == "血が死ん") {
                cd.skipFlagList!![cd.index + 1] = 1
                cd.convertedText.add("だ")
            }
        }
        return convertedFlag
    }

    /**
     * 「挟む」→「はさげる」の変換処理
     */
    private fun hasageruConvert(cd: ConverterData, parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.original == "挟む" && parsedData.lexicaCate == "動詞") {
            convertedFlag = getVerbConjugational(cd, parsedData, "挟む")
        }
        // 連用タ接続に変換する際、はさげ「だ」になるのを防ぐ処理
        if (convertedFlag) {
            if (parsedData.conjType == "連用タ接続") {
                cd.skipFlagList!![cd.index + 1] = 1
                cd.convertedText.add("た")
            }
        }
        return convertedFlag
    }

    /**
     * 「(壊れる、)使えなくなる、使えんくなる」→「ばかになる」の変換処理(「壊れる」は通常の動詞変換メソッドでOK)
     */
    private fun bakaniNaruConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.original == "なる" && parsedData.lexicaCate == "動詞") {
            if ((cd.parsedBeforeData?.surface == "なく" && cd.parsedBeforeData?.lexicaCate == "助動詞") ||
                (cd.parsedBeforeData?.surface == "く" && cd.parsedBeforeData?.lexicaCate == "動詞")
            ) {
                if (cd.parsedBeforeBeforeData?.original == "使える") {
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    convertedFlag = getVerbConjugational(cd, parsedData, "使えなくなる")
                }
            }
        }
        return convertedFlag
    }

    /**
     * 「不愉快な、不愉快だ、いやだ、いやな」→「いやったい」の変換処理
     */
    private fun iyattaiConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if ((parsedData.surface == "な" || parsedData.surface == "だ") && parsedData.lexicaCate == "助動詞") {
            if ((cd.parsedBeforeData?.surface == "いや" || cd.parsedBeforeData?.surface == "不愉快") && cd.parsedBeforeData?.lexicaCate == "名詞") {
                val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='不愉快な']]")
                cd.convertedText.removeAt(cd.convertedText.size - 1)
                cd.convertedText.add(ensyuWord[0].text)
                convertedFlag = true
            }
        }
        return convertedFlag
    }

    /**
     * 「水っぽい」→「しゃびしゃび」の変換処理
     */
    private fun shabishabiConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "水っぽい") {
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='水っぽい']]")
            // 「水っぽい」は形容詞、「しゃびしゃび」は名詞なので"な"を末尾に付加。「しゃびしゃび」をすばり表現する標準語が見当たらない
            cd.convertedText.add("${ensyuWord[0].text}な")
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「仕方がない、仕方ない、しょうがない」→「しょんない」の変換処理
     */
    private fun syonnaiConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "仕方がない" && parsedData.lexicaCate == "名詞") {
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='仕方がない']]")
            cd.convertedText.add("${ensyuWord[0].text}")
            convertedFlag = true
        } else if (parsedData.surface == "ない" && parsedData.lexicaCate == "助動詞") {
            if (cd.parsedBeforeData?.surface == "仕方" && cd.parsedBeforeData?.lexicaCate == "名詞") {
                val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='仕方ない']]")
                cd.convertedText.removeAt(cd.convertedText.size - 1)
                cd.convertedText.add("${ensyuWord[0].text}")
                convertedFlag = true
            }
        } else if (parsedData.surface == "ない" && parsedData.lexicaCate == "形容詞") {
            if (cd.parsedBeforeData?.surface == "しょうが" && cd.parsedBeforeData?.lexicaCate == "名詞") {
                val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='しょうがない']]")
                cd.convertedText.removeAt(cd.convertedText.size - 1)
                cd.convertedText.add("${ensyuWord[0].text}")
                convertedFlag = true
            }
        }
        return convertedFlag
    }

    /**
     * 「熱い」→「ちんちん」の変換処理
     */
    private fun tintinConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "熱い" && parsedData.lexicaCate == "形容詞") {
            if (cd.parsedNextData != null) {
                if (cd.parsedNextData!!.surface == "よ" && cd.parsedNextData!!.lexicaCate == "助詞") {
                    val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='熱い']]")
                    cd.convertedText.add("${ensyuWord[0].text}だ")
                    convertedFlag = true
                    return convertedFlag
                } else if (true) {

                }
            }
            val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='熱い']]")
            cd.convertedText.add("${ensyuWord[0].text}")
            convertedFlag = true
        }
        return convertedFlag
    }

    /**
     * 「すぐに、急いで」→「ちゃっちゃと」の変換処理
     */
    private fun tyattyatoConvert(cd: ConverterData, parsedDataList: ArrayList<ParseResultData>, parsedData: ParseResultData): Boolean {
        // 「すぐに」→「ちゃっちゃと」が適用されない文脈もある。(例.すぐに壁が立ちはだかった)
        var convertedFlag = false
        if (parsedData.surface == "すぐ" && parsedData.lexicaCate == "副詞") {
            if (cd.parsedNextData?.surface == "に" && cd.parsedNextData?.lexicaCate == "助詞") {
                val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='すぐに']]")
                cd.convertedText.add("${ensyuWord[0].text}")
                convertedFlag = true
                cd.skipFlagList!![cd.index + 1] = 1
                return convertedFlag
            }
        } else if (parsedData.surface == "で" && parsedData.lexicaCate == "助詞") {
            if (cd.parsedBeforeData?.surface == "急い" && cd.parsedBeforeData?.lexicaCate == "動詞" && cd.parsedBeforeData?.conjType == "連用タ接続") {
                val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='すぐに']]")
                cd.convertedText.removeAt(cd.convertedText.size - 1)
                cd.convertedText.add("${ensyuWord[0].text}")
                convertedFlag = true
                return convertedFlag
            }
        }
        return convertedFlag
    }

    /**
     * 「なくては、なくちゃ」→「にゃ」の変換処理
     */
    private fun nyaConvert(cd: ConverterData, parsedData: ParseResultData): Boolean {
        var convertedFlag = false
        if (parsedData.surface == "は" && parsedData.lexicaCate == "助詞") {
            if (cd.parsedBeforeData?.surface == "て" && cd.parsedBeforeData?.lexicaCate == "助詞") {
                if (cd.parsedBeforeBeforeData?.surface == "なく" && cd.parsedBeforeBeforeData?.lexicaCate == "助動詞") {
                    if (cd.parsed3BeforeData?.surface == "し" && cd.parsed3BeforeData?.lexicaCate == "動詞" && cd.parsed3BeforeData?.conjType == "未然形") {
                        val ensyuWord: List<Node> =
                            document.selectNodes("//enshu[../standard[text()='なくては']]")
                        cd.convertedText.removeAt(cd.convertedText.size - 1)
                        cd.convertedText.removeAt(cd.convertedText.size - 1)
                        cd.convertedText.add("${ensyuWord[0].text}")
                        convertedFlag = true
                    }
                }
            }
        } else if (parsedData.surface == "ちゃ" && parsedData.lexicaCate == "助詞") {
            if (cd.parsedBeforeData?.surface == "なく" && cd.parsedBeforeData?.lexicaCate == "助動詞") {
                if (cd.parsedBeforeBeforeData?.surface == "し" && cd.parsedBeforeBeforeData?.lexicaCate == "動詞" && cd.parsedBeforeBeforeData?.conjType == "未然形") {
                    val ensyuWord: List<Node> =
                        document.selectNodes("//enshu[../standard[text()='なくては']]")
                    cd.convertedText.removeAt(cd.convertedText.size - 1)
                    cd.convertedText.add("${ensyuWord[0].text}")
                    convertedFlag = true
                }
            }
        }
        return convertedFlag
    }

    /**
     * 各変換メソッドから呼ばれる共通変換処理。
     */
    private fun simplConvert(cd: ConverterData, parsedData: ParseResultData, standardWordList: List<Node>): Boolean {
        var convertedFlag = false
        for (standardWord in standardWordList) {
            if (standardWord.text == parsedData.surface) {
                // 標準語に対応した遠州弁を取得
                val ensyuWord: List<Node> = document.selectNodes("//enshu[../standard[text()='${standardWord.text}']]")
                cd.convertedText.add(ensyuWord[0].text)
                convertedFlag = true
            }
        }
        return convertedFlag
    }
}