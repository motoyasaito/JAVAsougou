package com.example.demo.controller.view;

import com.example.demo.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// 画面（このパッケージのController）で起きたエラーを、JSONではなくエラー画面で表示する
// GlobalExceptionHandler はJSONを返すので、画面のエラーはこちらが先に受け取る
@ControllerAdvice(basePackages = "com.example.demo.controller.view") // このパッケージのControllerで起きたエラーだけを受け持つ
@Order(Ordered.HIGHEST_PRECEDENCE) // GlobalExceptionHandler より先に使われるようにする（数字が小さいほど先）
public class ViewExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ViewExceptionHandler.class);

  // URLのパラメータの形が違うとき（例: ?page=a）
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST) // 画面を返すときも、ステータスコードは400にする
  public String handleTypeMismatch(MethodArgumentTypeMismatchException e, Model model) {
    model.addAttribute("message", "URLの指定が正しくありません");
    return "common/error";// /WEB-INF/jsp/common/error.jsp を表示する
  }

  // データが見つからないとき（例: 存在しない伝票番号の詳細を開いた）
  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleNotFound(NotFoundException e, Model model) {
    model.addAttribute("message", e.getMessage());// Serviceで決めたメッセージ（「該当する配送物が見つかりません」など）をそのまま出す
    return "common/error";
  }

  // 上のどれにも当てはまらない、想定外のエラー
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleUnexpected(Exception e, Model model) {
    log.error("画面の表示中に想定外のエラーが発生しました", e);// 原因はログにだけ出す
    model.addAttribute("message", "システムでエラーが発生しました。時間をおいて、もう一度お試しください");// 画面には中身を出さない
    return "common/error";
  }
}
