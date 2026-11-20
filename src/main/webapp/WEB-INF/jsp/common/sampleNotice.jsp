<%-- 処理がまだできていない画面に出す「準備中」の帯。Controller が sampleData を true で渡したときだけ出る --%>
<%-- 見本のデータを本物と間違えないようにするため。処理につないだら、Controller で sampleData を渡すのをやめれば消える --%>
<%-- 文を変えたい画面は、Controller から sampleNoticeMessage を渡す（渡さなければ下の決まった文） --%>
<%@ page pageEncoding="UTF-8" %>
<c:if test="${sampleData}">
  <div class="alert alert-warning" role="note">
    <strong>準備中</strong>：<c:out value="${empty sampleNoticeMessage
        ? 'この画面は表示の確認用です。表示しているのは見本のデータで、操作はまだできません。'
        : sampleNoticeMessage}" />
  </div>
</c:if>
