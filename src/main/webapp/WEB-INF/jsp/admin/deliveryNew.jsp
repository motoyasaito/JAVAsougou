<%-- 管理者の荷物登録画面。入力に間違いがあったときは、AdminDeliveryNewViewController から fieldErrors（項目ごとのメッセージ）を受け取る --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="deliveries" />
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>荷物登録 | 配送管理システム</title>
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
          <h1 class="page-title">荷物登録</h1>
        </div>

        <%-- 間違いがあったときは、上にまとめてお知らせを出す（項目ごとの理由は、各入力欄の下に出す） --%>
        <c:if test="${not empty fieldErrors}">
          <div class="alert alert-error" role="alert">入力内容に誤りがあります。赤字の項目を確認してください。</div>
        </c:if>

        <%-- 入力した値は param.項目名 で戻す。param は「今回送られてきたフォームの値」。最初に開いたときは空 --%>
        <%-- 値を入力欄に戻すときは fn:escapeXml で安全にする（XSS対策） --%>
        <%-- onsubmit：送るときに「登録」ボタンを押せなくして、ダブルクリックで2件登録されるのを防ぐ --%>
        <form action="/admin/deliveries" method="post" class="card form" novalidate
              onsubmit="this.querySelector('button[type=submit]').disabled = true;">

          <h2 class="card-title">お届け先</h2>

          <div class="form-row">
            <label for="name" class="form-label">配送先名 <span class="required">必須</span></label>
            <div class="form-field">
              <%-- 間違いがある項目には is-invalid を付けて、枠を赤くする --%>
              <input type="text" id="name" name="name" maxlength="50" autocomplete="off"
                     class="input ${not empty fieldErrors.name ? 'is-invalid' : ''}"
                     value="${fn:escapeXml(param.name)}" placeholder="例：山田 花子">
              <c:if test="${not empty fieldErrors.name}"><p class="field-error"><c:out value="${fieldErrors.name}" /></p></c:if>
            </div>
          </div>

          <div class="form-row">
            <label for="callNumber" class="form-label">電話番号 <span class="required">必須</span></label>
            <div class="form-field">
              <%-- type="tel" と inputmode="numeric"：スマホで数字のキーボードを出す --%>
              <input type="tel" id="callNumber" name="callNumber" maxlength="11" inputmode="numeric" autocomplete="off"
                     class="input ${not empty fieldErrors.callNumber ? 'is-invalid' : ''}"
                     value="${fn:escapeXml(param.callNumber)}" placeholder="例：09012345678">
              <p class="field-hint">ハイフンなしの10桁または11桁</p>
              <c:if test="${not empty fieldErrors.callNumber}"><p class="field-error"><c:out value="${fieldErrors.callNumber}" /></p></c:if>
            </div>
          </div>

          <div class="form-row">
            <label for="email" class="form-label">メールアドレス <span class="required">必須</span></label>
            <div class="form-field">
              <input type="email" id="email" name="email" maxlength="100" autocomplete="off"
                     class="input ${not empty fieldErrors.email ? 'is-invalid' : ''}"
                     value="${fn:escapeXml(param.email)}" placeholder="例：yamada@example.com">
              <c:if test="${not empty fieldErrors.email}"><p class="field-error"><c:out value="${fieldErrors.email}" /></p></c:if>
            </div>
          </div>

          <div class="form-row">
            <label for="address" class="form-label">お届け先住所 <span class="required">必須</span></label>
            <div class="form-field">
              <%-- textarea は value 属性ではなく、タグの間に値を書く --%>
              <textarea id="address" name="address" maxlength="100" rows="3"
                        class="input ${not empty fieldErrors.address ? 'is-invalid' : ''}"
                        placeholder="例：埼玉県さいたま市浦和区1-1-1">${fn:escapeXml(param.address)}</textarea>
              <c:if test="${not empty fieldErrors.address}"><p class="field-error"><c:out value="${fieldErrors.address}" /></p></c:if>
            </div>
          </div>

          <h2 class="card-title form-section">荷物と配達日時</h2>

          <div class="form-row">
            <label for="itemType" class="form-label">荷物の種類 <span class="required">必須</span></label>
            <div class="form-field">
              <select id="itemType" name="itemType" class="input ${not empty fieldErrors.itemType ? 'is-invalid' : ''}">
                <option value="">選択してください</option>
                <%-- 送る値はコード値（NORMAL など）、画面に見せるのは日本語 --%>
                <option value="NORMAL" ${param.itemType == 'NORMAL' ? 'selected' : ''}>通常</option>
                <option value="RAW FOOD" ${param.itemType == 'RAW FOOD' ? 'selected' : ''}>生もの</option>
                <option value="FROZEN" ${param.itemType == 'FROZEN' ? 'selected' : ''}>冷凍</option>
                <option value="FRAGILE" ${param.itemType == 'FRAGILE' ? 'selected' : ''}>割れ物</option>
              </select>
              <c:if test="${not empty fieldErrors.itemType}"><p class="field-error"><c:out value="${fieldErrors.itemType}" /></p></c:if>
            </div>
          </div>

          <div class="form-row">
            <label for="deliveryDate" class="form-label">配達日 <span class="required">必須</span></label>
            <div class="form-field">
              <%-- type="date"：カレンダーから選べる入力欄。2026-11-21 の形で送られる --%>
              <%-- min：カレンダーで選べるいちばん早い日（今日）。Controller の addMinDate で渡している --%>
              <input type="date" id="deliveryDate" name="deliveryDate" min="${minDate}"
                     class="input ${not empty fieldErrors.deliveryDate ? 'is-invalid' : ''}"
                     value="${fn:escapeXml(param.deliveryDate)}">
              <p class="field-hint">今日以降の日付を選んでください</p>
              <c:if test="${not empty fieldErrors.deliveryDate}"><p class="field-error"><c:out value="${fieldErrors.deliveryDate}" /></p></c:if>
            </div>
          </div>

          <div class="form-row">
            <label for="deliveryTime" class="form-label">時間帯 <span class="required">必須</span></label>
            <div class="form-field">
              <select id="deliveryTime" name="deliveryTime" class="input ${not empty fieldErrors.deliveryTime ? 'is-invalid' : ''}">
                <option value="">選択してください</option>
                <%-- 選べる時間帯は DeliveryRules.DELIVERY_TIMES（入力チェックと同じ。ViewConfig で全画面に配っている） --%>
                <c:forEach var="time" items="${deliveryTimes}">
                  <option value="${time}" ${param.deliveryTime == time ? 'selected' : ''}>${time}</option>
                </c:forEach>
              </select>
              <c:if test="${not empty fieldErrors.deliveryTime}"><p class="field-error"><c:out value="${fieldErrors.deliveryTime}" /></p></c:if>
            </div>
          </div>

          <div class="form-actions">
            <a href="/admin/deliveries" class="button">キャンセル</a>
            <button type="submit" class="button button-primary">登録</button>
          </div>
        </form>
      </main>
    </div>
  </div>
</body>
</html>
