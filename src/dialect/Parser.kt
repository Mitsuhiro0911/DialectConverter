package dialect

import java.io.BufferedReader
import java.io.InputStreamReader


class Parser {
    /**
     * 形態素解析し、出現した名詞のリストを返す。
     */
    fun parse(command: Array<String>): ArrayList<ParseResultData> {
        val parsedDataList = arrayListOf<ParseResultData>()
        // コマンド結果をProcessで受け取る
        val ps = Runtime.getRuntime().exec(command)
        // 標準出力
        val bReader_i = BufferedReader(InputStreamReader(ps.inputStream, "UTF-8"))
        // 標準出力を1行ずつ受け取る一時オブジェクト
        var targetLine: String?
        // 形態素解析結果を全て解析する
        while (true) {
            // 形態素解析結果を1行ずつ受け取る
            targetLine = bReader_i.readLine()
            // 最終行まで解析が完了したらループを抜ける
            if (targetLine == null) {
                break
            } else if (targetLine == "EOS") {
                continue
            } else {
                // 形態素解析結果
                val parseResult = targetLine.split("[\t|,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // 読み、発音情報がない単語もある。その場合要素数が少ないため、[8][9]の要素を指定するとArrayIndexOutOfBoundsExceptionで落ちる。
                val parsedData = ParseResultData.Builder(
                    surface = parseResult[0],
                    lexicaCate = parseResult[1],
                    lexicaCateClass1 = parseResult[2],
                    lexicaCateClass2 = parseResult[3],
                    lexicaCateClass3 = parseResult[4],
                    conjForm = parseResult[5],
                    conjType = parseResult[6],
                    original = parseResult[7]
                ).reading(parseResult).pronunciation(parseResult).builder()
                parsedDataList.add(parsedData)
            }
        }
        // 終了を待つ
        ps.waitFor()
        return parsedDataList
    }
}