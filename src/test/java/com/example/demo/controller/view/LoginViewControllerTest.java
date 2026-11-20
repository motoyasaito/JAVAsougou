package com.example.demo.controller.view;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

// ログイン画面のテスト（今は画面の表示だけ）
// TODO: ログインの処理（A001）をつないだら、ログインの成功・失敗のテストを足す
@WebMvcTest(LoginViewController.class)
class LoginViewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void ログイン画面を準備中の印つきで表示する() throws Exception {
    mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attribute("sampleData", true))
        .andExpect(model().attribute("sampleNoticeMessage", "ログイン機能は準備中です。下の仮の入口から各画面を確認できます。"));
  }
}
