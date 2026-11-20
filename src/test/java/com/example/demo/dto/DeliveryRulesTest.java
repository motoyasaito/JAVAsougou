package com.example.demo.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeliveryRulesTest {

  @Test
  void 時間帯の入力チェックとプルダウンの中身がそろっている() {
    // List を「|」でつないだものが、入力チェックの文字と同じであること。片方だけ直したら、このテストが落ちて気づける
    assertThat(String.join("|", DeliveryRules.DELIVERY_TIMES)).isEqualTo(DeliveryRules.DELIVERY_TIME_PATTERN);
  }

  @Test
  void 数字12桁なら伝票番号の形() {
    assertThat(DeliveryRules.isTrackingNumber("100000000001")).isTrue();
  }

  @Test
  void 桁が違う_数字以外_空なら伝票番号の形ではない() {
    assertThat(DeliveryRules.isTrackingNumber("12345")).isFalse();
    assertThat(DeliveryRules.isTrackingNumber("1000000000a1")).isFalse();
    assertThat(DeliveryRules.isTrackingNumber("")).isFalse();
    assertThat(DeliveryRules.isTrackingNumber(null)).isFalse();
  }
}
