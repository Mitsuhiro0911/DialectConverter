package dialect

data class ConverterData (
    // 遠州弁変換後のテキストデータ
    val convertedText: ArrayList<String>,
    // parsedDataListの参照中データの次のデータ
    var parsedNextData: ParseResultData? = null,
    // parsedDataListの参照中データの次の次のデータ
    var parsedNextNextData: ParseResultData? = null,
    // parsedDataListの参照中データの前のデータ
    var parsedBeforeData: ParseResultData? = null,
    // parsedDataListの参照中データの前の前のデータ
    var parsedBeforeBeforeData: ParseResultData? = null,
    // parsedDataListの参照中データの3つ前のデータ
    var parsed3BeforeData: ParseResultData? = null,
    // 変換処理の要・不要を判定するフラグ。parsedDataListの要素とインデックスが対応付いている。
    var skipFlagList: ArrayList<Int>? = null,
    // parsedDataListのインデックス
    var index: Int = 0
)