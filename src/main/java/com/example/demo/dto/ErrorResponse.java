package com.example.demo.dto;

import java.util.List;

public record ErrorResponse(// エラーが起きたときに返すレスポンスの形。
    int status, // HTTPのステータスコード
    String error, // ステータスの名前（BAD_Requestなど）
    String message, // 画面に出す人が読むためのメッセージ
    List<String> details) {//入力チェックで引っかかった項目の一覧。なければ空。List<String>は「文字列のリスト」という意味
}
