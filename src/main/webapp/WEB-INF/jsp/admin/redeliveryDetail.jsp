<%-- 管理者の再配達詳細画面。AdminRedeliveryViewController から redelivery（再配達）と drivers（ドライバー一覧）を受け取る --%>
<%-- 荷物詳細（deliveryDetail.jsp）と同じ形にそろえている --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 再配達一覧の中の画面なので、左メニューは「再配達一覧」を選んだ状態にする --%>
<c:set var="activeMenu" value="redeliveries" />
<%-- 割り振ったドライバーを、ドライバー一覧の中から探しておく（名前とエリアを出すため） --%>
<c:forEach var="driver" items="${drivers}">
  <c:if test="${driver.id == redelivery.assignedDriverId}">
    <c:set var="assignedDriver" value="${driver}" />
  </c:if>
</c:forEach>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>再配達詳細 | 配送管理システム</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div class="layout">
    <%@ include file="/WEB-INF/jsp/common/sidebar.jsp" %>
    <div class="main">
      <%@ include file="/WEB-INF/jsp/common/topbar.jsp" %>

      <main class="content">
        <a href="/admin/redeliveries" class="back-link">‹ 再配達一覧へ戻る</a>
        <div class="page-header">
          <h1 class="page-title">再配達詳細</h1>
        </div>

        <%@ include file="/WEB-INF/jsp/common/sampleNotice.jsp" %>
        <%@ include file="/WEB-INF/jsp/common/flashMessages.jsp" %>

        <%-- 再配達の情報 --%>
        <section class="card">
          <h2 class="card-title">再配達情報</h2>
          <dl class="detail-list">
            <dt>配送番号</dt>
            <dd><c:out value="${redelivery.trackingNumber}" /></dd>
            <dt>再配達の希望日時</dt>
            <dd>
              <ui:dateTime value="${redelivery.redeliveryDate}" pattern="yyyy/MM/dd（E）" />
              <span class="text-sub"><c:out value="${redelivery.redeliveryTime}" /></span>
            </dd>
            <dt>割り振り状況</dt>
            <dd>
              <c:choose>
                <c:when test="${redelivery.allocationStatus == 'UNASSIGNED'}"><span class="badge badge-yellow">未対応</span></c:when>
                <c:otherwise><span class="badge badge-green">割り振り済み</span></c:otherwise>
              </c:choose>
            </dd>
            <dt>配送物の状態</dt>
            <dd><ui:statusBadge status="${redelivery.deliveryStatus}" /></dd>
            <dt>荷物の種類</dt>
            <dd><ui:itemTypeLabel itemType="${redelivery.itemType}" /></dd>
            <dt>前回の担当</dt>
            <dd><c:out value="${redelivery.previousDriverName}" /></dd>
          </dl>
        </section>

        <%-- お届け先。個人情報なので、画面には出すがログには出さない --%>
        <section class="card">
          <h2 class="card-title">お届け先</h2>
          <dl class="detail-list">
            <dt>配送先名</dt>
            <dd><c:out value="${redelivery.name}" /></dd>
            <dt>住所</dt>
            <dd><c:out value="${redelivery.address}" /></dd>
            <dt>電話番号</dt>
            <dd><c:out value="${redelivery.callNumber}" /></dd>
            <dt>メールアドレス</dt>
            <dd><c:out value="${redelivery.email}" /></dd>
          </dl>
        </section>

        <%-- 再配達のドライバーの割り振り --%>
        <section class="card">
          <h2 class="card-title">再配達の担当ドライバー</h2>
          <dl class="detail-list">
            <dt>割り振り先</dt>
            <dd>
              <c:choose>
                <c:when test="${not empty assignedDriver}">
                  <c:out value="${assignedDriver.name}" />
                  <c:if test="${not empty assignedDriver.areaName}">
                    <span class="text-sub">（<c:out value="${assignedDriver.areaName}" />）</span>
                  </c:if>
                </c:when>
                <c:otherwise><span class="badge badge-yellow">未割り当て</span></c:otherwise>
              </c:choose>
            </dd>
          </dl>

          <%-- TODO: 再配達の割り振り（A011）が main に入ったら、POST /admin/redeliveries/{id}/driver につないで押せるようにする --%>
          <%-- A011 のルール：割り振れるのは「未対応」かつ「配送物が発送準備中」のとき。つなぐときは荷物詳細と同じく、それ以外ではフォームを出さない --%>
          <div class="inline-form">
            <label for="driverId" class="form-label">ドライバー</label>
            <select id="driverId" class="input" disabled>
              <option value="">選択してください</option>
              <c:forEach var="driver" items="${drivers}">
                <option value="${driver.id}" ${driver.id == redelivery.assignedDriverId ? 'selected' : ''}>
                  <c:out value="${driver.name}" /><c:if test="${not empty driver.areaName}">（<c:out value="${driver.areaName}" />）</c:if>
                </option>
              </c:forEach>
            </select>
            <button type="button" class="button button-primary" disabled>割り振る</button>
            <span class="field-hint">割り振りは準備中です</span>
          </div>
        </section>

        <%-- 更新情報。荷物詳細と同じく、状態の変わった履歴の代わりに、依頼された日時と最後に変わった日時を出す --%>
        <section class="card">
          <h2 class="card-title">更新情報</h2>
          <dl class="detail-list">
            <dt>依頼日時</dt>
            <dd><ui:dateTime value="${redelivery.requestedAt}" /></dd>
            <dt>最終更新日時</dt>
            <dd><ui:dateTime value="${redelivery.updatedAt}" /></dd>
          </dl>
        </section>
      </main>
    </div>
  </div>
</body>
</html>
