<%-- お客さん用の再配達依頼の入力画面 --%>
<%-- RedeliveryRequestViewController から受け取るもの： --%>
<%--   minDate（選べるいちばん早い日）、fieldErrors（項目ごとの間違い）、errorMessage（受け付けられなかった理由）、completed（受け付けた内容） --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>再配達依頼 | <c:out value="${customerBrandName}" /></title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body class="public-body">
  <%@ include file="/WEB-INF/jsp/common/publicHeader.jsp" %>

  <main class="public-content">
    <%-- 入力した値は param.項目名 で戻す。配送状況の画面から来たときは ?trackingNumber=... の値が入っている --%>
    <%-- onsubmit：送るときに「送信」ボタンを押せなくして、ダブルクリックで2回依頼されるのを防ぐ --%>
    <form action="/redelivery" method="post" class="card public-card" novalidate
          onsubmit="this.querySelector('button[type=submit]').disabled = true;">
      <h1 class="public-title">再配達依頼</h1>
      <p class="public-lead">ご不在の際にお持ち帰りしたお荷物の、再配達をご依頼いただけます。</p>

      <%-- 受け付けられなかったとき（番号と電話番号が合わない、不在ではない など） --%>
      <c:if test="${not empty errorMessage}">
        <div class="alert alert-error" role="alert"><c:out value="${errorMessage}" /></div>
      </c:if>
      <c:if test="${not empty fieldErrors}">
        <div class="alert alert-error" role="alert">入力内容に誤りがあります。赤字の項目を確認してください。</div>
      </c:if>

      <div class="form-row">
        <label for="trackingNumber" class="form-label">配送番号 <span class="required">必須</span></label>
        <div class="form-field">
          <input type="text" id="trackingNumber" name="trackingNumber" maxlength="12" inputmode="numeric" autocomplete="off"
                 class="input ${not empty fieldErrors.trackingNumber ? 'is-invalid' : ''}"
                 value="${fn:escapeXml(param.trackingNumber)}" placeholder="12桁の数字">
          <c:if test="${not empty fieldErrors.trackingNumber}"><p class="field-error"><c:out value="${fieldErrors.trackingNumber}" /></p></c:if>
        </div>
      </div>

      <div class="form-row">
        <label for="callNumber" class="form-label">電話番号 <span class="required">必須</span></label>
        <div class="form-field">
          <%-- ご本人の確認のため、荷物を登録したときの電話番号と合っているかを確かめる --%>
          <input type="tel" id="callNumber" name="callNumber" maxlength="11" inputmode="numeric" autocomplete="tel"
                 class="input ${not empty fieldErrors.callNumber ? 'is-invalid' : ''}"
                 value="${fn:escapeXml(param.callNumber)}" placeholder="例：09012345678">
          <p class="field-hint">ご本人確認に使います。お届け先の電話番号（ハイフンなし）</p>
          <c:if test="${not empty fieldErrors.callNumber}"><p class="field-error"><c:out value="${fieldErrors.callNumber}" /></p></c:if>
        </div>
      </div>

      <div class="form-row">
        <label for="redeliveryDate" class="form-label">希望日 <span class="required">必須</span></label>
        <div class="form-field">
          <%-- min：カレンダーで選べるいちばん早い日（明日）。今日以前は選べない --%>
          <input type="date" id="redeliveryDate" name="redeliveryDate" min="${minDate}"
                 class="input ${not empty fieldErrors.redeliveryDate ? 'is-invalid' : ''}"
                 value="${fn:escapeXml(param.redeliveryDate)}">
          <p class="field-hint">明日以降の日付を選んでください</p>
          <c:if test="${not empty fieldErrors.redeliveryDate}"><p class="field-error"><c:out value="${fieldErrors.redeliveryDate}" /></p></c:if>
        </div>
      </div>

      <div class="form-row">
        <label for="redeliveryTime" class="form-label">希望時間帯 <span class="required">必須</span></label>
        <div class="form-field">
          <select id="redeliveryTime" name="redeliveryTime" class="input ${not empty fieldErrors.redeliveryTime ? 'is-invalid' : ''}">
            <option value="">選択してください</option>
            <%-- 選べる時間帯は、CreateRedeliveryRequest の入力チェックと同じ5つ --%>
            <c:forEach var="time" items="午前中,12:00-14:00,14:00-16:00,16:00-18:00,18:00-20:00">
              <option value="${time}" ${param.redeliveryTime == time ? 'selected' : ''}>${time}</option>
            </c:forEach>
          </select>
          <c:if test="${not empty fieldErrors.redeliveryTime}"><p class="field-error"><c:out value="${fieldErrors.redeliveryTime}" /></p></c:if>
        </div>
      </div>

      <div class="public-actions">
        <button type="submit" class="button button-primary button-wide">送信</button>
      </div>
    </form>
  </main>

  <%-- 受け付けたあとに出す完了のモーダル。completed は、受け付けたときに Controller から1回だけ渡される --%>
  <c:if test="${not empty completed}">
    <%-- dialog：モーダル（画面の上に重ねて出す窓）を作る HTML のタグ。aria-labelledby で、読み上げソフトに窓の名前を伝える --%>
    <dialog id="completedDialog" class="modal" aria-labelledby="completedTitle">
      <h2 id="completedTitle" class="modal-title">依頼を受け付けました</h2>
      <dl class="detail-list">
        <dt>配送番号</dt>
        <dd><c:out value="${completed.trackingNumber}" /></dd>
        <dt>希望日</dt>
        <dd><c:out value="${fn:replace(completed.redeliveryDate, '-', '/')}" /></dd>
        <dt>希望時間帯</dt>
        <dd><c:out value="${completed.redeliveryTime}" /></dd>
      </dl>
      <div class="modal-actions">
        <%-- method="dialog" のフォームのボタンを押すと、モーダルが閉じる --%>
        <form method="dialog"><button class="button">閉じる</button></form>
        <a href="/tracking/status?trackingNumber=${completed.trackingNumber}" class="button button-primary">配送状況を確認する</a>
      </div>
    </dialog>
    <script>
      // ページを開いたらモーダルを出す。showModal() は、後ろの画面を操作できないようにして、Esc キーで閉じられるようにしてくれる
      document.getElementById('completedDialog').showModal();
    </script>
  </c:if>
</body>
</html>
