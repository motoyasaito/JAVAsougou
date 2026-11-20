package com.example.demo.dto;

import java.util.List;

// 配送物の入力の決まりごとを1か所にまとめたもの
// API の入力チェック（@Pattern）・画面の Controller・JSP のプルダウンが、すべてここを見る。
// 決まりを変えるときは、ここだけ直せば全部に反映される（以前は6か所に同じことが書いてあった）
public final class DeliveryRules {

  // 伝票番号の形：数字12桁
  // @Pattern(regexp = ...) に渡すので、「定数」（static final の文字列をそのまま書いたもの）にしておく必要がある
  public static final String TRACKING_NUMBER_PATTERN = "^[0-9]{12}$";

  // 配達の時間帯として選べるもの（プルダウンに並べる順）
  public static final List<String> DELIVERY_TIMES =
      List.of("午前中", "12:00-14:00", "14:00-16:00", "16:00-18:00", "18:00-20:00");

  // 配達の時間帯の入力チェック用（上の5つのどれか）
  // @Pattern には List を渡せないので、同じ内容を文字で書いている。DeliveryRulesTest で、上の List と合っているかを確かめている
  public static final String DELIVERY_TIME_PATTERN = "午前中|12:00-14:00|14:00-16:00|16:00-18:00|18:00-20:00";

  // 道具としてだけ使うので、new できないようにする
  private DeliveryRules() {
  }

  // 伝票番号の形（数字12桁）になっているか
  public static boolean isTrackingNumber(String value) {
    return value != null && value.matches(TRACKING_NUMBER_PATTERN);
  }
}
