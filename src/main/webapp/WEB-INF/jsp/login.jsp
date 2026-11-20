<%-- ログイン画面。管理者とドライバーが使う社内の画面なので、社内の名前（配送管理システム）を出す --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ログイン | 配送管理システム</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body class="login-body">
  <main class="login-content">
    <div class="login-brand">配送管理システム</div>

    <%-- ログインの処理ができるまでの「準備中」の帯 --%>
    <%@ include file="/WEB-INF/jsp/common/sampleNotice.jsp" %>

    <%-- TODO: ログインの処理（A001）ができたら、POST /login で送れるようにする（今はボタンを押せない） --%>
    <%-- 入力欄の名前（username・password）は、Spring Security の標準の名前に合わせている --%>
    <form action="/login" method="post" class="card login-card" novalidate>
      <h1 class="login-title">ログイン</h1>

      <div class="login-field">
        <label for="username" class="form-label">ログインID（メールアドレス）</label>
        <%-- autocomplete="username"：ブラウザに「これはログインID」と伝え、保存したIDを入れてもらえるようにする --%>
        <input type="email" id="username" name="username" class="input" autocomplete="username"
               placeholder="例：admin@example.com" maxlength="100">
      </div>

      <div class="login-field">
        <label for="password" class="form-label">パスワード</label>
        <%-- type="password"：入力した文字を ●●● で隠す。autocomplete="current-password"：保存したパスワードを入れてもらえる --%>
        <input type="password" id="password" name="password" class="input" autocomplete="current-password" maxlength="100">
      </div>

      <button type="submit" class="button button-primary login-button" disabled>ログイン</button>
    </form>

    <%-- TODO: ログインの処理をつないだら、この仮の入口は消す --%>
    <%-- ログインができるまで、各画面を確認するための仮の入口 --%>
    <div class="login-temporary">
      <p class="text-sub">ログイン機能ができるまでの仮の入口</p>
      <div class="login-temporary-links">
        <a href="/admin/deliveries" class="link">管理者の画面へ</a>
        <a href="/driver/deliveries" class="link">ドライバーの画面へ</a>
      </div>
    </div>
  </main>
</body>
</html>
