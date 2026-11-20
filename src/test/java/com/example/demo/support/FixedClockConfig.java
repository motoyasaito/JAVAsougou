package com.example.demo.support;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

// テストで使う「決まった日の時計」
// 本物の時計（TimeConfig）だと、テストを実行した日によって「今日」が変わり、夜0時をまたぐと結果がずれることがある。
// この時計なら、いつ実行しても「今日」は 2026-11-20 になる
@TestConfiguration // テストのときだけ使う設定。@Import(FixedClockConfig.class) を付けたテストで読み込まれる
public class FixedClockConfig {

  public static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");
  public static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

  // 2026-11-20 10:00（日本時間）で止まった時計を作る。Service の単体テストでは、これを直接使う
  public static Clock fixedClock() {
    return Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE);
  }

  // Controller のテスト（@WebMvcTest）では、Bean として本物の時計の代わりに入れる
  @Bean
  public Clock clock() {
    return fixedClock();
  }
}
