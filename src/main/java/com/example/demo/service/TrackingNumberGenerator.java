package com.example.demo.service;

import java.security.SecureRandom;//乱数を作るクラスを使えるようにするパッケージ。
import org.springframework.stereotype.Component;

// 伝票番号（12桁の数字）をランダムに作る。推測されないようSecureRandomを使う
@Component
public class TrackingNumberGenerator {
  private final SecureRandom random = new SecureRandom();// 乱数を作る道具を用意。

  public String generate() {// 伝票番号を一つ作って返すメソッド
    // 100000000000〜999999999999 の範囲にして、先頭が0にならないようにする
    long number = random.nextLong(100_000_000_000L, 1_000_000_000_000L);
    // random.nextLong(最小, 上限)：「最小以上、上限未満」の範囲で、ランダムな整数を1つ作ります。
    // 最小 100_000_000_000 → 12桁で一番小さい数（先頭が1）
    // 上限 1_000_000_000_000 → 13桁で一番小さい数。これは含まれないので、結果は最大でも
    // 999_999_999_999（12桁）になります。
    return String.valueOf(number);
  }
}
