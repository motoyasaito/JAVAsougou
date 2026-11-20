<%-- 配送ステータスを色付きのバッジで表示する部品（タグファイル）。 --%>
<%-- WEB-INF/tags に置いた .tag ファイルは、JSP で <ui:statusBadge status="..." /> のようにタグとして使える --%>
<%-- 複数の画面で同じ表示をするので、1か所にまとめておく --%>
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%-- attribute：このタグが受け取る値。required="true" は「必ず渡す」という意味 --%>
<%@ attribute name="status" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
  <c:when test="${status == 'PREPARING'}"><span class="badge badge-gray">発送準備中</span></c:when>
  <c:when test="${status == 'IN TRANSIT'}"><span class="badge badge-blue">配達中</span></c:when>
  <c:when test="${status == 'DELIVERED'}"><span class="badge badge-green">配達完了</span></c:when>
  <c:when test="${status == 'ABSENT'}"><span class="badge badge-red">不在</span></c:when>
  <%-- 決まった値以外が来たときは、そのまま出す（c:out で安全に表示） --%>
  <c:otherwise><span class="badge badge-gray"><c:out value="${status}" /></span></c:otherwise>
</c:choose>
