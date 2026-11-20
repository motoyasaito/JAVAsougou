<%-- 処理がまだできていない画面に出す「準備中」の帯。Controller が sampleData を true で渡したときだけ出る --%>
<%-- 見本のデータを本物と間違えないようにするため。処理につないだら、Controller で sampleData を渡すのをやめれば消える --%>
<%@ page pageEncoding="UTF-8" %>
<c:if test="${sampleData}">
  <div class="alert alert-warning" role="note">
    <strong>準備中</strong>：この画面は表示の確認用です。表示しているのは見本のデータで、操作はまだできません。
  </div>
</c:if>
