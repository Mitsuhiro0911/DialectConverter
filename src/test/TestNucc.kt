package test

import dialect.Converter
import dialect.Parser
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

fun main () {
    val br = BufferedReader(FileReader(File("./data/test_data/input/data002.txt")))
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
        Converter().convert(parsedDataList)
        inputText = br.readLine()
    }
}