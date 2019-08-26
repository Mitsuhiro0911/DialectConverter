package dialect

fun main () {
    val inputText = "お爺ちゃんあそこにおいてあるペン取って。"
    val command = arrayOf(
        "sh", "-c",
        "echo ${inputText} | mecab -d /usr/local/lib/mecab/dic/mecab-ipadic-neologd"
    )
    val parsedDataList = Parser().parse(command)
    Converter().convert(parsedDataList)
}