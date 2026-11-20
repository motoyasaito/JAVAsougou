package com.example.demo.controller.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

class FieldErrorsTest {

  // テスト用の入力チェックの結果を作る（中身は FieldErrors.from が見る項目だけでよい）
  private BindingResult bindingResult() {
    return new BeanPropertyBindingResult(new Object(), "form");
  }

  // 1件分の間違い。code は引っかかったチェックの名前、bindingFailure は型が合わなかったかどうか
  private FieldError error(String field, String code, String message, boolean bindingFailure) {
    return new FieldError("form", field, null, bindingFailure, new String[] {code}, null, message);
  }

  @Test
  void 空のときは後から来ても入力してくださいを優先する() {
    BindingResult result = bindingResult();
    result.addError(error("callNumber", "Pattern", "電話番号はハイフンなしの10桁または11桁の数字で入力してください", false));
    result.addError(error("callNumber", "NotBlank", "電話番号を入力してください", false));

    assertThat(FieldErrors.from(result)).containsEntry("callNumber", "電話番号を入力してください");
  }

  @Test
  void 空のときは先に来ても入力してくださいのまま変えない() {
    BindingResult result = bindingResult();
    result.addError(error("callNumber", "NotBlank", "電話番号を入力してください", false));
    result.addError(error("callNumber", "Pattern", "電話番号はハイフンなしの10桁または11桁の数字で入力してください", false));

    assertThat(FieldErrors.from(result)).containsEntry("callNumber", "電話番号を入力してください");
  }

  @Test
  void 型が合わないときは決まった文にする() {
    BindingResult result = bindingResult();
    result.addError(error("deliveryDate", "typeMismatch", "Failed to convert property value ...", true));

    assertThat(FieldErrors.from(result)).containsEntry("deliveryDate", "形式が正しくありません");
  }

  @Test
  void 項目ごとに1つずつまとめる() {
    BindingResult result = bindingResult();
    result.addError(error("name", "NotBlank", "お届け先の氏名を入力してください", false));
    result.addError(error("email", "Email", "メールアドレスの形式が正しくありません", false));

    Map<String, String> fieldErrors = FieldErrors.from(result);

    assertThat(fieldErrors).hasSize(2)
        .containsEntry("name", "お届け先の氏名を入力してください")
        .containsEntry("email", "メールアドレスの形式が正しくありません");
  }
}
