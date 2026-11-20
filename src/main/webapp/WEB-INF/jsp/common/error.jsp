<%-- エラー画面。ViewExceptionHandler・PageErrorResolver から message と area（外枠：admin / driver / public）を受け取って表示する --%>
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
  <%-- ブラウザのタブの名前も、社内の画面（管理者・ドライバー）とお客さんの画面で分ける --%>
  <title>エラー | ${area == 'public' ? fn:escapeXml(customerBrandName) : '配送管理システム'}</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <c:choose>
    <%-- 管理者の画面で起きたエラー：左メニューのある外枠 --%>
    <c:when test="${area == 'admin'}">
      <div class="layout">
        <%@ include file="/WEB-INF/jsp/common/sidebar.jsp" %>
        <div class="main">
          <%@ include file="/WEB-INF/jsp/common/topbar.jsp" %>
          <main class="content">
            <div class="card error-card">
              <h1 class="page-title">エラーが発生しました</h1>
              <%-- c:out は、値に < や > が入っていても、HTMLとして動かないように変換して表示する（XSS対策） --%>
              <p class="message-error"><c:out value="${message}" /></p>
              <div class="error-actions">
                <%-- history.back()：ブラウザの「戻る」と同じ。エラーはどの画面からでも起きるので、来た画面に戻れるようにする --%>
                <button type="button" class="button" onclick="history.back()">‹ 前の画面に戻る</button>
                <%-- URL を直接打って来たときなど、前の画面がないときのための逃げ道 --%>
                <a href="/admin/deliveries" class="button">配送物一覧へ</a>
              </div>
            </div>
          </main>
        </div>
      </div>
    </c:when>
    <%-- ドライバーの画面で起きたエラー：社内用の、メニューのない外枠 --%>
    <c:when test="${area == 'driver'}">
      <header class="driver-header">
        <div class="driver-brand">配送管理システム</div>
        <form action="/logout" method="post" class="topbar-logout">
          <button type="submit" class="button button-small">ログアウト</button>
        </form>
      </header>
      <main class="driver-content">
        <div class="card error-card">
          <h1 class="page-title">エラーが発生しました</h1>
          <p class="message-error"><c:out value="${message}" /></p>
          <div class="error-actions">
            <button type="button" class="button" onclick="history.back()">‹ 前の画面に戻る</button>
            <a href="/driver/deliveries" class="button">本日の配達物へ</a>
          </div>
        </div>
      </main>
    </c:when>
    <%-- お客さんの画面で起きたエラー：お客さん向けの名前の外枠 --%>
    <c:otherwise>
      <%@ include file="/WEB-INF/jsp/common/publicHeader.jsp" %>
      <main class="public-content">
        <div class="card error-card">
          <h1 class="page-title">エラーが発生しました</h1>
          <p class="message-error"><c:out value="${message}" /></p>
          <div class="error-actions">
            <button type="button" class="button" onclick="history.back()">‹ 前の画面に戻る</button>
            <a href="/tracking" class="button">配送状況の確認へ</a>
          </div>
        </div>
      </main>
    </c:otherwise>
  </c:choose>
</body>
</html>
