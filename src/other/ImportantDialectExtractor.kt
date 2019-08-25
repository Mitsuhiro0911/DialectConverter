package other

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

fun main () {
    println("""
<?xml version="1.0" encoding="UTF-8"?>
<dialect>
    """.trimMargin())
    write("3")
    write("2")
    write("1")
    println("</dialect>")
}

fun write (importance: String) {
    val br = BufferedReader(FileReader(File("./data/corpas/dialect_data.xml")))
    var str = br.readLine()
    while (str != null) {
        if (str.contains("<word>")) {
            var importanceFlag = false
            val output = ArrayList<String>()
            while (!str.contains("</word>")) {
                if (str.contains("<importance>${importance}</importance>")) {
                    importanceFlag = true
                }
                output.add(str)
                str = br.readLine()
            }
            output.add(str)
            if (importanceFlag) {
                for (out in output) {
                    println(out)
                }
            }
        }
        str = br.readLine()
    }
}