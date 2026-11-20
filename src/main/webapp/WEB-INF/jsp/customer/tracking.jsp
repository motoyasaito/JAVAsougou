<%-- お客さん用の配送番号の入力画面。見つからなかったときは、TrackingViewController から errorMessage と trackingNumber を受け取る --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>配送状況確認 | <c:out value="${customerBrandName}" /></title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body class="public-body">
  <%@ include file="/WEB-INF/jsp/common/publicHeader.jsp" %>

  <main class="public-content">
    <div class="card public-card">
      <h1 class="public-title">配送状況確認</h1>
      <p class="public-lead">お手元の伝票に書かれた、12桁の配送番号を入力してください。</p>

      <%-- GET で送るので、結果の画面のURL（/tracking/status?trackingNumber=...）をブックマークや共有にも使える --%>
      <form action="/tracking/status" method="get" class="tracking-form">
        <label for="trackingNumber" class="visually-hidden">配送番号</label>
        <input type="text" id="trackingNumber" name="trackingNumber" maxlength="12" inputmode="numeric"
               autocomplete="off" placeholder="配送番号を入力してください（12桁）"
               class="input tracking-input ${not empty errorMessage ? 'is-invalid' : ''}"
               value="${fn:escapeXml(trackingNumber)}">
        <button type="submit" class="button button-primary">検索</button>
      </form>
      <c:if test="${not empty errorMessage}">
        <p class="field-error" role="alert"><c:out value="${errorMessage}" /></p>
      </c:if>
    </div>
  </main>
</body>
</html>
