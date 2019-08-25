package other

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

fun main () {
    val br = BufferedReader(FileReader(File("./data/rough_data")))
    var str = br.readLine()
    while (str != null) {
        if (str.contains("☆")) {
//            println(str)
            println("""
    <word>
        <standard>${str.split("\t")[2]}</standard>
        <enshu>${str.split("\t")[1]}</enshu>
        <lexicaCategory></lexicaCategory>
        <conjugational>
            <kihon></kihon>
            <mizen></mizen>
            <mizen_u></mizen_u>
            <renyo></renyo>
            <katei></katei>
            <meirei></meirei>
        </conjugational>
    </word>
            """.trimMargin())
        }
        str = br.readLine()
    }
}