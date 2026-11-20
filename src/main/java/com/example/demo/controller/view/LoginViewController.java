package com.example.demo.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// ログイン画面を受け持つController（画面の表示だけ）
// TODO: ログインの処理（A001）を作るときにつなぐ。Spring Security を使うなら、
//   ・この画面をログイン画面として設定する（loginPage("/login")）
//   ・ログインの受け付け（POST /login）とログアウトは Spring Security に任せる（LogoutViewController は消す）
//   ・フォームに CSRF のトークンを足す
@Controller
public class LoginViewController {

  // ログイン画面を表示する（ログアウトしたあとも、ここに移る）
  @GetMapping("/login")
  public String login(Model model) {
    model.addAttribute("sampleData", true);// 「準備中」の帯を出すための印（ログインの処理をつないだら消す）
    model.addAttribute("sampleNoticeMessage", "ログイン機能は準備中です。下の仮の入口から各画面を確認できます。");// 帯の文（ログイン画面用）
    return "login";// /WEB-INF/jsp/login.jsp を表示する
  }
}
