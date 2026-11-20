package com.example.demo.controller.view;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

// ログアウトの画面操作を受け持つController
// TODO: ログイン機能を作るときに、認証の仕組みに合わせて作り直す（Spring Security を使うなら、ログアウトはその機能に任せる）
@Controller
public class LogoutViewController {

  // 上の帯の「ログアウト」ボタン（POST /logout）で呼ばれる
  @PostMapping("/logout")
  public String logout(HttpSession session) {// HttpSession：ブラウザごとに、サーバーが覚えておく情報の入れ物（ログイン中の人など）
    session.invalidate();// セッションを消す。中に入っていた情報（ログイン中の人など）も全部なくなる
    return "redirect:/login";// ログイン画面へ移る。ログイン画面はこのあと作る
  }
}
