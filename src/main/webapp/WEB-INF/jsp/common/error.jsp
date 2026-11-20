<%-- エラー画面。ViewExceptionHandler から message と adminPage（管理者の画面かどうか）を受け取って表示する --%>
<%-- contentType：ブラウザに「HTML、文字コードはUTF-8」と伝える --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- JSTL の基本タグ（c:out など）を「c:」という名前で使えるようにする --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <%-- ブラウザのタブの名前も、管理者の画面とお客さんの画面で分ける --%>
  <title>エラー | ${adminPage ? '配送管理システム' : fn:escapeXml(customerBrandName)}</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <c:choose>
    <%-- 管理者の画面で起きたエラー：左メニューのある外枠 --%>
    <c:when test="${adminPage}">
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
    </c:when>
    <%-- お客さんの画面で起きたエラー：メニューのない外枠 --%>
    <c:otherwise>
      <%@ include file="/WEB-INF/jsp/common/publicHeader.jsp" %>
      <main class="public-content">
        <div class="card error-card">
          <h1 class="page-title">エラーが発生しました</h1>
          <p class="message-error"><c:out value="${message}" /></p>
          <a href="/tracking" class="button">配送状況の確認へ戻る</a>
        </div>
      </main>
    </c:otherwise>
  </c:choose>
</body>
</html>
