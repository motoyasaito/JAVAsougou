<%-- 登録・更新のあとに出すメッセージ。Controller が successMessage / errorMessage をフラッシュ属性で渡したときだけ出る --%>
<%@ page pageEncoding="UTF-8" %>
<c:if test="${not empty successMessage}">
  <%-- role="status"：読み上げソフトに「結果のお知らせ」だと伝える --%>
  <div class="alert alert-success" role="status"><c:out value="${successMessage}" /></div>
</c:if>
<c:if test="${not empty errorMessage}">
  <%-- role="alert"：読み上げソフトに、すぐ読み上げるべきエラーだと伝える --%>
  <div class="alert alert-error" role="alert"><c:out value="${errorMessage}" /></div>
</c:if>
