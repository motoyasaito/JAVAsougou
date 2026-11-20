<%-- 管理者の配送物一覧画面。AdminDeliveryViewController から result（PageResponse）を受け取って表示する --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- c：繰り返しや条件分岐のタグ。fn：文字列を扱う関数（置き換え・エスケープなど） --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- ui：WEB-INF/tags に作った自分の部品（statusBadge など）を使えるようにする --%>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 左メニューで「配送物一覧」を選ばれた状態にする（sidebar.jsp がこの値を見る） --%>
<c:set var="activeMenu" value="deliveries" />
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>配送物一覧 | 配送管理システム</title>
  <%-- CSS は src/main/resources/static/css/style.css に置く。static の下は /css/... のURLでそのまま読める --%>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div class="layout">
    <%-- 左メニューと上の帯を、この場所に読み込む --%>
    <%@ include file="/WEB-INF/jsp/common/sidebar.jsp" %>
    <div class="main">
      <%@ include file="/WEB-INF/jsp/common/topbar.jsp" %>

      <main class="content">
        <%-- 画面のタイトルと、いちばん使うボタン（荷物を登録） --%>
        <div class="page-header">
          <h1 class="page-title">配送物一覧</h1>
          <a href="/admin/deliveries/new" class="button button-primary">＋ 荷物を登録</a>
        </div>

        <div class="card">
          <%-- 伝票番号の検索。GET で /admin/deliveries/search?trackingNumber=... を開く --%>
          <form action="/admin/deliveries/search" method="get" class="search-form">
            <label for="trackingNumber" class="search-label">伝票番号で探す</label>
            <%-- value には、検索して見つからなかったときに入力した値を戻す。属性の中は fn:escapeXml で安全にする --%>
            <input type="text" id="trackingNumber" name="trackingNumber" class="input"
                   value="${fn:escapeXml(trackingNumber)}" placeholder="12桁の数字" maxlength="12" inputmode="numeric">
            <button type="submit" class="button">検索</button>
          </form>
          <%-- searchError は、検索で見つからなかったときに Controller から1回だけ渡される --%>
          <c:if test="${not empty searchError}">
            <p class="message-error"><c:out value="${searchError}" /></p>
          </c:if>
        </div>

        <div class="card">
          <c:choose>
            <%-- empty は「中身が空か」を調べる。1件もなければメッセージだけを出す --%>
            <c:when test="${empty result.content}">
              <p class="message-empty">配送物はありません</p>
            </c:when>
            <c:otherwise>
              <%-- 「全25件中 11〜20件」のように、今どこを見ているかを出す。page は0始まりなので計算する --%>
              <c:set var="from" value="${result.page * result.size + 1}" />
              <c:set var="to" value="${result.page * result.size + fn:length(result.content)}" />
              <p class="table-count">全 ${result.totalElements} 件中 ${from}〜${to} 件</p>

              <table class="table">
                <thead>
                  <tr>
                    <th>伝票番号</th>
                    <th>お届け先</th>
                    <th>品目</th>
                    <th>配達日時</th>
                    <th>ドライバー</th>
                    <th>状態</th>
                  </tr>
                </thead>
                <tbody>
                  <%-- c:forEach は繰り返し。result.content の中身を1件ずつ delivery という名前で取り出して、1行ずつ作る --%>
                  <c:forEach var="delivery" items="${result.content}">
                    <tr>
                      <%-- 伝票番号から詳細画面へのリンク。詳細画面はこのあと作る --%>
                      <td><a href="/admin/deliveries/${delivery.trackingNumber}" class="link"><c:out value="${delivery.trackingNumber}" /></a></td>
                      <%-- 氏名などの入力された値は、必ず c:out で表示する（XSS対策） --%>
                      <td><c:out value="${delivery.name}" /></td>
                      <%-- 自分で作った部品を使う。中で NORMAL → 通常 のように変換している --%>
                      <td><ui:itemTypeLabel itemType="${delivery.itemType}" /></td>
                      <%-- 2026-10-23 を 2026/10/23 にして、時間帯と並べる --%>
                      <td>
                        <c:out value="${fn:replace(delivery.deliveryDate, '-', '/')}" />
                        <span class="text-sub"><c:out value="${delivery.deliveryTime}" /></span>
                      </td>
                      <td>
                        <c:choose>
                          <%-- まだ割り当てていない配送物は、管理者が気づけるように目立たせる --%>
                          <c:when test="${empty delivery.driverId}"><span class="badge badge-yellow">未割り当て</span></c:when>
                          <c:otherwise>ID <c:out value="${delivery.driverId}" /></c:otherwise>
                        </c:choose>
                      </td>
                      <td><ui:statusBadge status="${delivery.status}" /></td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </c:otherwise>
          </c:choose>

          <%-- ページの移動。2ページ以上あるときだけ出す --%>
          <c:if test="${result.totalPages > 1}">
            <nav class="pager">
              <%-- 1ページ目でなければ「前へ」を押せる --%>
              <c:choose>
                <c:when test="${result.page > 0}"><a href="?page=${result.page - 1}" class="pager-item">‹ 前へ</a></c:when>
                <c:otherwise><span class="pager-item is-disabled">‹ 前へ</span></c:otherwise>
              </c:choose>

              <%-- ページ番号を並べる。begin〜end の数を i に入れて繰り返す。画面には +1 して1から見せる --%>
              <c:forEach var="i" begin="0" end="${result.totalPages - 1}">
                <c:choose>
                  <c:when test="${i == result.page}"><span class="pager-item is-current">${i + 1}</span></c:when>
                  <c:otherwise><a href="?page=${i}" class="pager-item">${i + 1}</a></c:otherwise>
                </c:choose>
              </c:forEach>

              <%-- 最後のページでなければ「次へ」を押せる --%>
              <c:choose>
                <c:when test="${result.page + 1 < result.totalPages}"><a href="?page=${result.page + 1}" class="pager-item">次へ ›</a></c:when>
                <c:otherwise><span class="pager-item is-disabled">次へ ›</span></c:otherwise>
              </c:choose>
            </nav>
          </c:if>
        </div>
      </main>
    </div>
  </div>
</body>
</html>
