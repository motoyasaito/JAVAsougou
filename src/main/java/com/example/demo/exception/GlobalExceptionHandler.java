package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;//ErrorResponseのDTOを使えるようにしている
import java.util.List;//detailの一覧を作成する
import org.slf4j.Logger;//ログ作成のためのアイテム
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;//ステータスコードを名前で扱うためのもの
import org.springframework.http.ResponseEntity;//ステータスコードとレスポンスの中身をまとめて返すためのクラス
import org.springframework.web.bind.MethodArgumentNotValidException;//@vaidの入力チェックで引っかかった時に、springが投げる例外クラス
import org.springframework.web.bind.annotation.ExceptionHandler;//
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // すべてのコントローラーで起きた例外を、このクラスで受けとる。例外が起きたときにこの中のメソッドを呼ぶ
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);// このクラス用のログを出す道具作り
  // 入力チェックが引っかかった時

  @ExceptionHandler(MethodArgumentNotValidException.class) // MethodArgumentNotValidException(入力チェックのエラー)が起きたら下のメソッドを呼ぶ
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {// 戻り値がResponseEntity<ErrorResponse>、「中身がErrorResponseのレスポンス」という意味になっている。引数eには起きた例外が入ってくる。どの項目がなぜ引っかかったかの情報を持っている。
    List<String> details = e.getBindingResult().getFieldErrors().stream()// 引っかかった項目を文字列の一覧に変える。
        .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
        .toList();

    return build(HttpStatus.BAD_REQUEST, "入力内容に誤りがあります", details);// bulidメソッドにステータス（400）、メッセージ、項目の一覧を渡して、できたレスポンスを返す。
  }

  @ExceptionHandler(Exception.class) // 上記で当てはまらないエラーをこっちで受けとる。Exceptionはほぼすべての例外クラスの親になる。
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {// エラー内容をログに出す。eを渡すことによってどこで何が起きたかまで残る。
    log.error("想定外のエラーが発生しました", e);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "サーバーでエラーが発生しました", List.of());// 決まった文言を返す
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, List<String> details) {// 2つのメソッドが同じ組み立てになるため、共通メソッドにする。
    ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, details);
    return ResponseEntity.status(status).body(body);
  }
}
