package com.example.demo.controller.view;

// エラー画面の外枠を、URL から決める道具
// 管理者・ドライバーは社内の人なので社内の名前（配送管理システム）、お客さんにはお客さん向けの名前（テスト配送）を見せる
public final class ErrorPageArea {

  public static final String ADMIN = "admin";// 管理者の画面（左メニューのある外枠）
  public static final String DRIVER = "driver";// ドライバーの画面（社内用の、メニューのない外枠）
  public static final String PUBLIC = "public";// お客さんの画面（お客さん向けの外枠）

  // 道具としてだけ使うので、new できないようにする
  private ErrorPageArea() {
  }

  // URL の先頭で、どの外枠にするかを決める
  public static String from(String requestUri) {
    if (requestUri.startsWith("/admin")) {
      return ADMIN;
    }
    if (requestUri.startsWith("/driver")) {
      return DRIVER;
    }
    return PUBLIC;
  }
}
