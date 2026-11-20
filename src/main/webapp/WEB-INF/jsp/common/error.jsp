<%-- エラー画面。ViewExceptionHandler から message を受け取って表示する --%>
<%-- contentType：ブラウザに「HTML、文字コードはUTF-8」と伝える --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- JSTL の基本タグ（c:out など）を「c:」という名前で使えるようにする --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>エラー | 配送管理システム</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div class="layout">
    <%@ include file="/WEB-INF/jsp/common/sidebar.jsp" %>
    <div class="main">
      <%@ include file="/WEB-INF/jsp/common/topbar.jsp" %>
      <main class="content">
        <div class="card error-card">
          <h1 class="page-title">エラーが発生しました</h1>
          <%-- c:out は、値に < や > が入っていても、HTMLとして動かないように変換して表示する（XSS対策） --%>
          <p class="message-error"><c:out value="${message}" /></p>
          <a href="/admin/deliveries" class="button">配送物一覧へ戻る</a>
        </div>
      </main>
    </div>
  </div>
</body>
</html>
