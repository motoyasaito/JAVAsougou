<%-- 管理者の配送物詳細画面。AdminDeliveryDetailViewController から delivery（配送物）と drivers（ドライバー一覧）を受け取る --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 詳細画面も「配送物」の中の画面なので、左メニューは配送物一覧を選んだ状態にする --%>
<c:set var="activeMenu" value="deliveries" />
<%-- 今の担当ドライバーを、ドライバー一覧の中から探しておく（名前とエリアを出すため） --%>
<c:forEach var="driver" items="${drivers}">
  <c:if test="${driver.id == delivery.driverId}">
    <c:set var="currentDriver" value="${driver}" />
  </c:if>
</c:forEach>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>荷物詳細 | 配送管理システム</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div class="layout">
    <%@ include file="/WEB-INF/jsp/common/sidebar.jsp" %>
    <div class="main">
      <%@ include file="/WEB-INF/jsp/common/topbar.jsp" %>

      <main class="content">
        <a href="/admin/deliveries" class="back-link">‹ 配送物一覧へ戻る</a>
        <div class="page-header">
          <h1 class="page-title">荷物詳細</h1>
        </div>

        <%-- 割り当てたあとの「割り当てました」や、断られたときのメッセージ --%>
        <%@ include file="/WEB-INF/jsp/common/flashMessages.jsp" %>

        <%-- 配送の情報 --%>
        <section class="card">
          <h2 class="card-title">配送情報</h2>
          <%-- dl / dt / dd：「項目名と値」の組を並べるためのHTMLのタグ。dt が項目名、dd が値 --%>
          <dl class="detail-list">
            <dt>伝票番号</dt>
            <dd><c:out value="${delivery.trackingNumber}" /></dd>
            <dt>状態</dt>
            <dd><ui:statusBadge status="${delivery.status}" /></dd>
            <dt>荷物の種類</dt>
            <dd><ui:itemTypeLabel itemType="${delivery.itemType}" /></dd>
            <dt>配達日時</dt>
            <dd>
              <c:out value="${fn:replace(delivery.deliveryDate, '-', '/')}" />
              <span class="text-sub"><c:out value="${delivery.deliveryTime}" /></span>
            </dd>
          </dl>
        </section>

        <%-- お届け先。個人情報なので、画面には出すがログには出さない --%>
        <section class="card">
          <h2 class="card-title">お届け先</h2>
          <dl class="detail-list">
            <dt>配送先名</dt>
            <dd><c:out value="${delivery.name}" /></dd>
            <dt>住所</dt>
            <dd><c:out value="${delivery.address}" /></dd>
            <dt>電話番号</dt>
            <dd><c:out value="${delivery.callNumber}" /></dd>
            <dt>メールアドレス</dt>
            <dd><c:out value="${delivery.email}" /></dd>
          </dl>
        </section>

        <%-- 担当ドライバーと割り当て --%>
        <section class="card">
          <h2 class="card-title">担当ドライバー</h2>
          <dl class="detail-list">
            <dt>現在の担当</dt>
            <dd>
              <c:choose>
                <c:when test="${empty delivery.driverId}"><span class="badge badge-yellow">未割り当て</span></c:when>
                <c:when test="${not empty currentDriver}">
                  <c:out value="${currentDriver.name}" />
                  <%-- エリアが決まっていないドライバーは、カッコごと出さない --%>
                  <c:if test="${not empty currentDriver.areaName}">
                    <span class="text-sub">（<c:out value="${currentDriver.areaName}" />）</span>
                  </c:if>
                </c:when>
                <%-- ドライバー一覧に見つからないとき（ドライバーでなくなった人など）は ID だけ出す --%>
                <c:otherwise>ID <c:out value="${delivery.driverId}" /></c:otherwise>
              </c:choose>
            </dd>
          </dl>

          <c:choose>
            <%-- 割り当てられるのは発送準備中のときだけ（Service と同じルール）。それ以外はフォームを出さない --%>
            <c:when test="${delivery.status == 'PREPARING'}">
              <%-- 割り当ては POST で送る。送り先は /admin/deliveries/{伝票番号}/driver --%>
              <%-- onsubmit：送るときに「割り振る」ボタンを押せなくして、ダブルクリックで2回送られるのを防ぐ --%>
              <%-- 押せなくなるのは送信中だけ。詳細画面が開き直すとまた押せるので、別のドライバーへの割り振り直しはできる --%>
              <form action="/admin/deliveries/${delivery.trackingNumber}/driver" method="post" class="inline-form"
                    onsubmit="this.querySelector('button[type=submit]').disabled = true;">
                <label for="driverId" class="form-label">ドライバー</label>
                <select id="driverId" name="driverId" class="input" required>
                  <%-- value="" の選択肢は「まだ選んでいない」。required があるので、このまま押すとブラウザが止める --%>
                  <option value="">選択してください</option>
                  <c:forEach var="driver" items="${drivers}">
                    <%-- 今の担当者を最初から選んだ状態（selected）にする --%>
                    <option value="${driver.id}" ${driver.id == delivery.driverId ? 'selected' : ''}>
                      <c:out value="${driver.name}" /><c:if test="${not empty driver.areaName}">（<c:out value="${driver.areaName}" />）</c:if>
                    </option>
                  </c:forEach>
                </select>
                <button type="submit" class="button button-primary">割り振る</button>
              </form>
            </c:when>
            <c:otherwise>
              <p class="text-sub">発送準備中の配送物のみ、ドライバーを割り当てられます。</p>
            </c:otherwise>
          </c:choose>
        </section>

        <%-- 更新情報。Figma の「ステータス変更履歴」の場所 --%>
        <%-- 状態の変わった履歴は今は残していないので、登録された日時と最後に変わった日時だけを出す --%>
        <%-- TODO: 履歴テーブルを作ったら、ここを「いつ・何に変わったか」の表に置き換える --%>
        <section class="card">
          <h2 class="card-title">更新情報</h2>
          <dl class="detail-list">
            <dt>登録日時</dt>
            <dd><ui:dateTime value="${detail.createdAt}" /></dd>
            <dt>最終更新日時</dt>
            <dd><ui:dateTime value="${detail.updatedAt}" /></dd>
          </dl>
        </section>
      </main>
    </div>
  </div>
</body>
</html>
