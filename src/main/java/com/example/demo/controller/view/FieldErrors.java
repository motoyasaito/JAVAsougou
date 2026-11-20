package com.example.demo.controller.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

// 画面の入力チェックの結果を、JSP で使いやすい形にする道具（荷物登録・再配達依頼などで共通）
public final class FieldErrors {

  // 「入力されていない」ことを表すチェックの名前（@NotBlank・@NotNull・@NotEmpty）
  private static final Set<String> REQUIRED_CODES = Set.of("NotBlank", "NotNull", "NotEmpty");

  // 道具としてだけ使うので、new できないようにする
  private FieldErrors() {
  }

  // 入力チェックの結果を「項目名 → メッセージ」の形にする。JSP で ${fieldErrors.name} のように項目ごとに出せる
  // 1つの項目に出すメッセージは1つだけ。空のときは「入力してください」を優先する
  // （空の電話番号は @NotBlank と @Pattern の両方に引っかかり、どちらが先に来るかは決まっていないため）
  public static Map<String, String> from(BindingResult bindingResult) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();// LinkedHashMap：入れた順番を覚えておく Map
    for (FieldError error : bindingResult.getFieldErrors()) {
      // isBindingFailure()：値の型が合わなかったとき（日付に「abc」など）。Springの英語のメッセージは出さず、決まった文にする
      String message = error.isBindingFailure() ? "形式が正しくありません" : error.getDefaultMessage();

      if (REQUIRED_CODES.contains(error.getCode())) {// getCode()：引っかかったチェックの名前（"NotBlank" など）
        fieldErrors.put(error.getField(), message);// 「入力してください」は、先に入っていたメッセージを上書きしてでも出す
      } else {
        fieldErrors.putIfAbsent(error.getField(), message);// それ以外は、まだ入っていないときだけ入れる
      }
    }
    return fieldErrors;
  }
}
