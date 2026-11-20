<%-- お客さん用の画面のヘッダー。ログインしない画面なので、メニューやログアウトは出さない --%>
<%-- 社内の呼び名（配送管理システム）ではなく、お客さん向けの会社・サービスの名前を出す --%>
<%-- customerBrandName は application.properties の app.customer-brand-name（ViewConfig で全画面に共有している） --%>
<%@ page pageEncoding="UTF-8" %>
<header class="public-header">
  <a href="/tracking" class="public-brand"><c:out value="${customerBrandName}" /></a>
</header>
