<%-- ドライバーの「本日の配達物」の画面。スマホで見る前提で、1件を1枚のカードにして縦に並べる --%>
<%-- DriverDeliveryViewController から受け取るもの： --%>
<%--   drivers（切り替え用のドライバー一覧）、driver（表示中のドライバー）、date（表示する日）、today（今日）、deliveries（配達物）、total（件数） --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>本日の配達物 | 配送管理システム</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body class="driver-body">
  <header class="driver-header">
    <div class="driver-brand">配送管理システム</div>
    <div class="driver-header-right">
      <%-- ログインができるまでの仮：ドライバーを選ぶと、その人の一覧に切り替わる --%>
      <%-- onchange：選んだら、そのままフォームを送る（ボタンを押さなくてよい） --%>
      <form action="/driver/deliveries" method="get" class="driver-switch">
        <label for="driverId" class="driver-switch-label">担当者（仮）</label>
        <select id="driverId" name="driverId" class="input input-small" onchange="this.form.submit()">
          <option value="">選択してください</option>
          <c:forEach var="d" items="${drivers}">
            <option value="${d.id}" ${d.id == driver.id ? 'selected' : ''}><c:out value="${d.name}" /></option>
          </c:forEach>
        </select>
        <%-- 今見ている日付を保ったまま、担当者だけ切り替える --%>
        <input type="hidden" name="date" value="${date}">
      </form>
      <form action="/logout" method="post" class="topbar-logout">
        <button type="submit" class="button button-small">ログアウト</button>
      </form>
    </div>
  </header>

  <main class="driver-content">
    <c:choose>
      <%-- ドライバーが決まっていないとき --%>
      <c:when test="${empty driver}">
        <div class="card message-card">
          <p>上の「担当者（仮）」から、ドライバーを選んでください。</p>
          <p class="text-sub">ログイン機能ができたら、ログインした人の一覧が自動で出るようになります。</p>
        </div>
      </c:when>
      <c:otherwise>
        <%-- 日付と件数。前の日・次の日に移れる --%>
        <div class="driver-date-bar">
          <a href="?driverId=${driver.id}&date=${date.minusDays(1)}" class="button button-small">‹ 前の日</a>
          <div class="driver-date">
            <h1 class="driver-title">${date == today ? '本日の配達物' : '配達物'}</h1>
            <%-- E は曜日（月・火…）。日本語の曜日にするため、タグに pattern を渡している --%>
            <span class="text-sub"><ui:dateTime value="${date}" pattern="M/d（E）" /> ・ 全 ${total} 件</span>
          </div>
          <a href="?driverId=${driver.id}&date=${date.plusDays(1)}" class="button button-small">次の日 ›</a>
        </div>
        <c:if test="${date != today}">
          <p class="driver-back-today"><a href="?driverId=${driver.id}" class="link">今日に戻る</a></p>
        </c:if>

        <c:choose>
          <c:when test="${empty deliveries}">
            <div class="card message-card"><p>この日の配達物はありません。</p></div>
          </c:when>
          <c:otherwise>
            <%-- 配達物のカード。押すとその配達物の詳細のモーダルが開く --%>
            <ul class="delivery-cards">
              <c:forEach var="delivery" items="${deliveries}" varStatus="loop">
                <li>
                  <%-- button にすると、キーボードでも押せる。onclick で、同じ番号のモーダルを開く --%>
                  <button type="button" class="delivery-card" onclick="document.getElementById('detail-${loop.index}').showModal()">
                    <span class="delivery-card-top">
                      <span class="delivery-card-time"><c:out value="${delivery.deliveryTime}" /></span>
                      <ui:statusBadge status="${delivery.status}" />
                    </span>
                    <span class="delivery-card-name"><c:out value="${delivery.name}" /> 様</span>
                    <span class="delivery-card-address"><c:out value="${delivery.address}" /></span>
                    <span class="delivery-card-meta">
                      <c:out value="${delivery.trackingNumber}" /> ・ <ui:itemTypeLabel itemType="${delivery.itemType}" />
                    </span>
                  </button>

                  <%-- この配達物の詳細のモーダル。Figma の「ドライバーモーダル画面」 --%>
                  <dialog id="detail-${loop.index}" class="modal" aria-labelledby="detail-title-${loop.index}">
                    <h2 id="detail-title-${loop.index}" class="modal-title">荷物詳細</h2>
                    <dl class="detail-list">
                      <dt>配送先名</dt>
                      <dd><c:out value="${delivery.name}" /> 様</dd>
                      <dt>配送番号</dt>
                      <dd><c:out value="${delivery.trackingNumber}" /></dd>
                      <dt>お届け住所</dt>
                      <dd><c:out value="${delivery.address}" /></dd>
                      <dt>電話番号</dt>
                      <%-- tel: のリンクにすると、スマホで押したときに電話がかかる（不在のときの連絡に使う） --%>
                      <dd><a href="tel:${fn:escapeXml(delivery.callNumber)}" class="link"><c:out value="${delivery.callNumber}" /></a></dd>
                      <dt>時間帯</dt>
                      <dd><c:out value="${delivery.deliveryTime}" /></dd>
                      <dt>荷物の種類</dt>
                      <dd><ui:itemTypeLabel itemType="${delivery.itemType}" /></dd>
                      <dt>状態</dt>
                      <dd><ui:statusBadge status="${delivery.status}" /></dd>
                    </dl>

                    <%-- 配送ステータスの変更 --%>
                    <%-- TODO: ステータス変更の API（A007、deliveryStatusChange ブランチ）が main にマージされたら、押せるようにしてつなぐ --%>
                    <div class="status-change">
                      <label for="status-${loop.index}" class="form-label">配送ステータス</label>
                      <div class="status-change-row">
                        <select id="status-${loop.index}" class="input" disabled>
                          <option ${delivery.status == 'PREPARING' ? 'selected' : ''}>発送準備中</option>
                          <option ${delivery.status == 'IN TRANSIT' ? 'selected' : ''}>配達中</option>
                          <option ${delivery.status == 'DELIVERED' ? 'selected' : ''}>配達完了</option>
                          <option ${delivery.status == 'ABSENT' ? 'selected' : ''}>不在</option>
                        </select>
                        <button type="button" class="button button-primary" disabled>更新</button>
                      </div>
                      <p class="field-hint">ステータスの変更は準備中です</p>
                    </div>

                    <div class="modal-actions">
                      <%-- autofocus：モーダルを開いたときに、最初にこのボタンを選んだ状態にする（電話番号のリンクを誤って押さないように） --%>
                      <form method="dialog"><button class="button" autofocus>閉じる</button></form>
                    </div>
                  </dialog>
                </li>
              </c:forEach>
            </ul>
          </c:otherwise>
        </c:choose>
      </c:otherwise>
    </c:choose>
  </main>
</body>
</html>
