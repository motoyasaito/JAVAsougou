<%-- 管理者の再配達一覧画面。AdminRedeliveryViewController から redeliveries（再配達の一覧）を受け取る --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 左メニューで「再配達一覧」を選ばれた状態にする --%>
<c:set var="activeMenu" value="redeliveries" />
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>再配達一覧 | 配送管理システム</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div class="layout">
    <%@ include file="/WEB-INF/jsp/common/sidebar.jsp" %>
    <div class="main">
      <%@ include file="/WEB-INF/jsp/common/topbar.jsp" %>

      <main class="content">
        <div class="page-header">
          <h1 class="page-title">再配達一覧</h1>
        </div>

        <%-- 処理ができるまでの「準備中」の帯 --%>
        <%@ include file="/WEB-INF/jsp/common/sampleNotice.jsp" %>

        <div class="card">
          <c:choose>
            <c:when test="${empty redeliveries}">
              <p class="message-empty">再配達の依頼はありません</p>
            </c:when>
            <c:otherwise>
              <table class="table">
                <thead>
                  <tr>
                    <th>配送番号</th>
                    <th>再配達の希望日時</th>
                    <th>依頼日時</th>
                    <th>前回の担当</th>
                    <th>状態</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="r" items="${redeliveries}">
                    <tr>
                      <%-- 配送番号から再配達詳細の画面へ。URL は再配達の id（1つの配送物に再配達が何回もありうるため） --%>
                      <td><a href="/admin/redeliveries/${r.id}" class="link"><c:out value="${r.trackingNumber}" /></a></td>
                      <td>
                        <ui:dateTime value="${r.redeliveryDate}" pattern="yyyy/MM/dd（E）" />
                        <span class="text-sub"><c:out value="${r.redeliveryTime}" /></span>
                      </td>
                      <td><ui:dateTime value="${r.requestedAt}" /></td>
                      <td><c:out value="${r.previousDriverName}" /></td>
                      <td>
                        <%-- 未対応は黄色で目立たせる（管理者がまず割り振るべきもの） --%>
                        <c:choose>
                          <c:when test="${r.allocationStatus == 'UNASSIGNED'}"><span class="badge badge-yellow">未対応</span></c:when>
                          <c:when test="${r.allocationStatus == 'DONE'}"><span class="badge badge-green">割り振り済み</span></c:when>
                          <c:otherwise><span class="badge badge-gray"><c:out value="${r.allocationStatus}" /></span></c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </c:otherwise>
          </c:choose>
        </div>
      </main>
    </div>
  </div>
</body>
</html>
