package com.example.demo.controller.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LogoutViewController.class)
class LogoutViewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void ログアウトするとセッションを消してログイン画面へ移る() throws Exception {
    MockHttpSession session = new MockHttpSession();// テスト用のセッション。ログイン中の状態の代わり

    mockMvc.perform(post("/logout").session(session))
        .andExpect(redirectedUrl("/login"));

    assertThat(session.isInvalid()).isTrue();// セッションが消えている
  }

  @Test
  void GETではログアウトできない() throws Exception {
    mockMvc.perform(get("/logout"))
        .andExpect(status().isMethodNotAllowed());// 405。リンクや画像から勝手にログアウトさせられない
  }
}
