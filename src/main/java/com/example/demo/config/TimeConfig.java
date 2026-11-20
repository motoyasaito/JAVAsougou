package com.example.demo.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 「今」を知るための時計（Clock）の設定
// これまでは、あちこちで LocalDate.now(ZoneId.of("Asia/Tokyo")) と書いていた。
// 時計を1つの部品にして、必要なクラスに渡す（コンストラクタで受け取る）形にすると、
//   ・日本時間の指定が1か所にまとまる
//   ・テストでは「決まった日の時計」を渡せるので、いつ実行しても同じ結果になる（夜0時をまたいでも落ちない）
@Configuration
public class TimeConfig {

  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of("Asia/Tokyo"));// 本物の時計（日本時間）。LocalDate.now(clock) で日本時間の今日が取れる
  }
}
