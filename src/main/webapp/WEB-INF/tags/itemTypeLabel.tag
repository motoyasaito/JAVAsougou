<%-- 品目のコード値（NORMAL など）を日本語で表示する部品（タグファイル） --%>
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="itemType" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
  <c:when test="${itemType == 'NORMAL'}">通常</c:when>
  <c:when test="${itemType == 'RAW FOOD'}">生もの</c:when>
  <c:when test="${itemType == 'FROZEN'}">冷凍</c:when>
  <c:when test="${itemType == 'FRAGILE'}">割れ物</c:when>
  <c:otherwise><c:out value="${itemType}" /></c:otherwise>
</c:choose>
