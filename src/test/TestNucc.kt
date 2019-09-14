package test

import dialect.Converter
import dialect.Parser
import java.io.*

fun main () {
    val br = BufferedReader(FileReader(File("./data/test_data/input/data001.txt")))
    val bw = BufferedWriter(OutputStreamWriter(FileOutputStream(File("./data/test_data/output/outdata001.csv")), "Shift-JIS"))
    var inputText = br.readLine()
    // ヘッダ部の読み飛ばし
    while (inputText.substring(0, 1) == "＠") {
        inputText = br.readLine()
    }

    while (inputText != "＠ＥＮＤ") {
        val command = arrayOf(
            "sh", "-c",
            "echo ${inputText.substring(5, inputText.length)} | mecab -d /usr/local/lib/mecab/dic/mecab-ipadic-neologd"
        )
        val parsedDataList = Parser().parse(command)
        val convertedText = Converter().convert(parsedDataList)
        var outputText = ""
        for (text in convertedText) {
            outputText = "${outputText}${text}"
        }
        // 遠州弁変換処理が行われた場合
        if (inputText.substring(5, inputText.length) != outputText) {
            bw.write("${inputText.substring(5, inputText.length)}")
            bw.newLine()
            bw.write("${outputText}")
            bw.newLine()
        }
        inputText = br.readLine()
    }
    bw.close()
}