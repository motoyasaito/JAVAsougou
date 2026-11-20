<%-- 管理者画面の上の帯。ログイン中の人の名前とログアウトを出す場所 --%>
<%@ page pageEncoding="UTF-8" %>
<header class="topbar">
  <%-- TODO: ログイン機能ができたら、ログイン中の管理者の名前を出す --%>
  <span class="topbar-user">管理者</span>
  <%-- ログアウトはリンク（GET）ではなくフォーム（POST）にする。 --%>
  <%-- GET だと、ほかのサイトに埋め込まれた画像などから勝手にログアウトさせられるおそれがあるため --%>
  <form action="/logout" method="post" class="topbar-logout">
    <button type="submit" class="button button-small">ログアウト</button>
  </form>
</header>
