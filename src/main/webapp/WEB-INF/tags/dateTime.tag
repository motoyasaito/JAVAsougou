<%-- 日時を「2026/11/20 10:15」の形で表示する部品（タグファイル） --%>
<%-- LocalDateTime をそのまま出すと「2026-11-20T10:15:30.123456」のようになり読みにくいため --%>
<%-- trimDirectiveWhitespaces：<%@ ... %> の行のあとに残る空行を消す（出力に余計な空白を出さない） --%>
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%-- import：このタグの中で DateTimeFormatter（日時の書き方を決める道具）を使えるようにする --%>
<%@ tag import="java.time.format.DateTimeFormatter" %>
<%-- Locale：言語の指定。曜日（E）を「金」のように日本語で出すため。指定しないとサーバーの設定によっては「Fri」になる --%>
<%@ tag import="java.util.Locale" %>
<%-- type：受け取る値の型。LocalDateTime も LocalDate も受け取れるように、共通の親の型にしている --%>
<%@ attribute name="value" required="true" type="java.time.temporal.TemporalAccessor" %>
<%@ attribute name="pattern" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- pattern を渡さなければ、日付と時刻（分まで）にする --%>
<c:set var="format" value="${empty pattern ? 'yyyy/MM/dd HH:mm' : pattern}" />
<%-- 値が空（null）なら何も出さない --%>
<c:if test="${not empty value}">${DateTimeFormatter.ofPattern(format, Locale.JAPANESE).format(value)}</c:if>
