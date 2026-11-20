package com.example.demo.controller.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorPageAreaTest {

  @Test
  void adminで始まれば管理者の外枠() {
    assertThat(ErrorPageArea.from("/admin/deliveries/999999999999")).isEqualTo("admin");
  }

  @Test
  void driverで始まればドライバーの外枠() {
    assertThat(ErrorPageArea.from("/driver/deliveries")).isEqualTo("driver");
  }

  @Test
  void それ以外はお客さんの外枠() {
    assertThat(ErrorPageArea.from("/tracking/status")).isEqualTo("public");
    assertThat(ErrorPageArea.from("/abc")).isEqualTo("public");
  }
}
