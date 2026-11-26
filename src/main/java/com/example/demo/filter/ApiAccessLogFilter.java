package com.example.demo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// APIが呼ばれるたびに、リクエストIDを付けて、アクセスログを1行出すFilter
// FilterはControllerより前に動く「入口の係」。すべてのリクエストがここを通る
@Component // Springに登録する。登録すると、リクエストが来たときに自動で呼ばれる
public class ApiAccessLogFilter extends OncePerRequestFilter {// OncePerRequestFilterを継承すると、1回のリクエストで1回だけ動くFilterになる
  private static final Logger accessLog = LoggerFactory.getLogger("API_ACCESS");// アクセスログ用のロガー。logback-spring.xmlで、"API_ACCESS"のログはapi-access.logに出す設定にしている

  // /api で始まらないリクエストは、このfilterを通さない
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    return !request.getRequestURI().startsWith("/api");// URLが/apiで始まらなければtrueを返す。trueのときはこのFilterを素通りする
  }

  // リクエストが来るたびに呼ばれる。ここでリクエストIDを付けて、処理が終わったらアクセスログを出す
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString().substring(0, 8);// 重ならないランダムな文字列を作り、長いので先頭8文字だけ使う
    MDC.put("requestId", requestId);// MDCに入れると、このリクエストの処理中に出すログすべてにIDが付く（xmlの%X{requestId}がこの値を読む）
    long start = System.currentTimeMillis();// 処理を始めた時刻（ミリ秒）を覚えておく
    try {
      filterChain.doFilter(request, response);// 次へ進める。この中でController → Service → GlobalExceptionHandlerが動く
    } finally {// 処理が成功しても失敗しても、最後に必ずここを通る
      long time = System.currentTimeMillis() - start;// 今の時刻 − 始めた時刻 ＝ 処理にかかった時間
      int status = response.getStatus();// 返すHTTPステータスコード（200、404など）を取る
      String message = request.getMethod() + " " + request.getRequestURI() + " " + status + " " + time + "ms";// 例: GET /api/health 200 15ms。本文は個人情報を含むので入れない

      if (status >= 500) {// 5xx（システム側の問題）はERROR。500は「400以上」にも当てはまるので、大きいほうから判定する
        accessLog.error(message);
      } else if (status >= 400) {// 4xx（利用者側の誤り）はWARN
        accessLog.warn(message);
      } else {// それ以外（2xxなど）はINFO
        accessLog.info(message);
      }
      MDC.remove("requestId");// IDを消す。スレッドは使い回されるので、消し忘れると次のリクエストに前のIDが付いてしまう。ログを出した後に消す
    }
  }
}
