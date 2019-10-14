# DialectConverter
## アプリケーション概要
標準語を入力すると遠州弁に変換することができます。

## アプリケーション作成目的

## プログラム構成

### 処理の流れ
①変換したい文をハードコーディング(Main.kt)

②変換したい文を形態素解析(Parser.kt)

③形態素解析結果を保持(ParseResultData.kt)

④形態素解析結果を元に単語を遠州弁に変換し、コンソール出力(Converter.kt、ConverterData.kt)
　
 
　④'前後の文脈情報を解析(ContextProcessor.kt)


## 使用技術
### 技術一覧
・Kotlin

・形態素解析器(mecab-ipadic-NEologd)

### 補足
◎ライブラリ

・jaxen-1.1.6

・dom4j-2.1.1

◎開発環境

・OS:macOS Mojave バージョン10.14.6

・IDE:IntelliJ IDEA Version2019.1.1

## 開発期間
2019年8月〜
