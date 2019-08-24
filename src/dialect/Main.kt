package dialect

fun main () {
    val inputText = "これはペンです。"
    val command = arrayOf(
        "sh", "-c",
        "echo ${inputText} | mecab -d /usr/local/lib/mecab/dic/mecab-ipadic-neologd"
    )
    val word = Parser().parse(command)
}