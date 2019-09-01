package other

import org.dom4j.Node
import org.dom4j.io.SAXReader

fun main() {
    val reader = SAXReader()
    val document = reader.read("./data/corpas/dialect_data.xml")
    val dataList: List<Node> = document.selectNodes("//enshu[../importance[text()='3']]")
    for (data in dataList) {
        println(data.text)
    }

}