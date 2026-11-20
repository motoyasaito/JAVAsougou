<%-- お客さん用の配送状況の画面。TrackingViewController から delivery（伝票番号・配達予定日・時間帯・状態）を受け取る --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- 状態から、進み具合の「今いる段階」を決める。0：発送準備中、1：配達中、2：配達完了 または 不在 --%>
<c:choose>
  <c:when test="${delivery.status == 'PREPARING'}"><c:set var="currentStep" value="0" /></c:when>
  <c:when test="${delivery.status == 'IN TRANSIT'}"><c:set var="currentStep" value="1" /></c:when>
  <c:otherwise><c:set var="currentStep" value="2" /></c:otherwise>
</c:choose>
<c:set var="absent" value="${delivery.status == 'ABSENT'}" />
<c:set var="delivered" value="${delivery.status == 'DELIVERED'}" />
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>配送状況 | <c:out value="${customerBrandName}" /></title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body class="public-body">
  <%@ include file="/WEB-INF/jsp/common/publicHeader.jsp" %>

  <main class="public-content">
    <div class="card public-card">
      <h1 class="public-title">配送状況</h1>

      <dl class="detail-list">
        <dt>配送番号</dt>
        <dd><c:out value="${delivery.trackingNumber}" /></dd>
        <dt>到着予定日</dt>
        <dd>
          <c:out value="${fn:replace(delivery.deliveryDate, '-', '/')}" />
          <span class="text-sub"><c:out value="${delivery.deliveryTime}" /></span>
        </dd>
      </dl>

      <%-- 進み具合。ol は「順番のある並び」を表すタグ。読み上げソフトにも順番が伝わる --%>
      <ol class="steps">
        <%-- 各段階に、終わった（is-done）・今ここ（is-current）・不在（is-absent）の印を付けて、CSS で色を変える --%>
        <li class="step ${currentStep > 0 || delivered ? 'is-done' : ''} ${currentStep == 0 ? 'is-current' : ''}">
          <span class="step-dot"></span>
          <span class="step-label">発送準備中</span>
        </li>
        <li class="step ${currentStep > 1 || delivered ? 'is-done' : ''} ${currentStep == 1 ? 'is-current' : ''}">
          <span class="step-dot"></span>
          <span class="step-label">配達中</span>
        </li>
        <li class="step ${delivered ? 'is-done is-current' : ''} ${absent ? 'is-absent is-current' : ''}">
          <span class="step-dot"></span>
          <%-- 最後の段階は、不在なら「不在」、それ以外は「配達完了」と出す --%>
          <span class="step-label">${absent ? '不在' : '配達完了'}</span>
        </li>
      </ol>

      <%-- 今の状態の説明 --%>
      <div class="status-message ${absent ? 'is-absent' : ''}">
        <c:choose>
          <c:when test="${delivery.status == 'PREPARING'}">お荷物の発送を準備しています。</c:when>
          <c:when test="${delivery.status == 'IN TRANSIT'}">お荷物を配達しています。到着までしばらくお待ちください。</c:when>
          <c:when test="${delivered}">お荷物の配達が完了しました。</c:when>
          <c:when test="${absent}">ご不在のため、お荷物を持ち帰りました。再配達をご依頼ください。</c:when>
        </c:choose>
      </div>

      <div class="public-actions">
        <a href="/tracking" class="button">別の配送番号を調べる</a>
        <%-- 不在のときだけ、再配達の依頼へ進むボタンを出す。再配達依頼入力画面はこのあと作る --%>
        <c:if test="${absent}">
          <a href="/redelivery?trackingNumber=${delivery.trackingNumber}" class="button button-primary">再配達を依頼する</a>
        </c:if>
      </div>
    </div>
  </main>
</body>
</html>
