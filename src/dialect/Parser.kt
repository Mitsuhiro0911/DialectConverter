package dialect

import java.io.BufferedReader
import java.io.InputStreamReader


class Parser {
    /**
     * 形態素解析し、出現した名詞のリストを返す。
     */
    fun parse(command: Array<String>): ArrayList<String> {
        val wordList = ArrayList<String>()
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
                // 品詞
                val targetType =
                    targetLine.split("[\t|,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                val word = targetLine.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                wordList.add(word)
                println(targetLine)
            }
        }
        // 終了を待つ
        ps.waitFor()
        return wordList
    }
}