<%-- 管理者画面の左メニュー。各画面で activeMenu を決めてから include すると、その項目が選ばれた色になる --%>
<%@ page pageEncoding="UTF-8" %>
<aside class="sidebar">
  <div class="sidebar-brand">配送管理システム</div>
  <nav class="sidebar-nav">
    <%-- activeMenu が 'deliveries' なら is-active を付けて、今いる画面のメニューを色付きにする --%>
    <%-- メニューには一覧などの「場所」だけを並べる。荷物登録は配送物一覧の「＋ 荷物を登録」ボタンから行く --%>
    <%-- 荷物登録画面や詳細画面でも activeMenu を 'deliveries' にして、「配送物一覧」を選ばれた状態にする --%>
    <a href="/admin/deliveries" class="sidebar-link ${activeMenu == 'deliveries' ? 'is-active' : ''}">配送物一覧</a>
    <a href="/admin/redeliveries" class="sidebar-link ${activeMenu == 'redeliveries' ? 'is-active' : ''}">再配達一覧</a>
  </nav>
</aside>
