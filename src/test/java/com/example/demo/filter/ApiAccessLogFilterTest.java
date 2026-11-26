package com.example.demo.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.MDC;

// Springを起動せず、偽物のリクエストとレスポンスをFilterに直接渡して、出たログを確かめる
@ExtendWith(OutputCaptureExtension.class) //コンソールに出た文字を捕まえられるようにする
class ApiAccessLogFilterTest {

  ApiAccessLogFilter apiAccessLogFilter = new ApiAccessLogFilter();// Springを使わないので、テストするFilterを自分でnewして作る

  @Test
  void ステータスが200ならINFOでアクセスログを出す(CapturedOutput output) throws Exception {// 引数にCapturedOutputを書くと、テスト中にコンソールへ出た文字が入って渡される
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");// 偽物のリクエスト。「GET /api/health が呼ばれた」ことにする
    MockHttpServletResponse response = new MockHttpServletResponse();// 偽物のレスポンス
    FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(200);// Controllerの代わり。Filterが「次へ進める」とこのラムダが呼ばれ、ステータスを200にする

    // When
    apiAccessLogFilter.doFilter(request, response, chain);// Filterを動かす。本物と同じく、shouldNotFilterの判定を通ってからdoFilterInternalが呼ばれる

    // Then
    assertThat(output).contains("INFO");// コンソールの出力にINFOが含まれている＝INFOでログが出た
    assertThat(output).contains("GET /api/health 200");// メソッド・URL・ステータスがログに出ている
  }

  @Test
  void ステータスが404ならWARNでアクセスログを出す(CapturedOutput output) throws Exception {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/deliveries/999");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(404);// 「見つからなかった」場合を再現するため、ステータスを404にする

    // When
    apiAccessLogFilter.doFilter(request, response, chain);

    // Then
    assertThat(output).contains("WARN");// 4xxはWARNで出る
    assertThat(output).contains("GET /api/deliveries/999 404");
  }

  @Test
  void ステータスが500ならERRORでアクセスログを出す(CapturedOutput output) throws Exception {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/deliveries");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(500);// 「システム側でエラーが起きた」場合を再現するため、ステータスを500にする

    // When
    apiAccessLogFilter.doFilter(request, response, chain);

    // Then
    assertThat(output).contains("ERROR");// 5xxはERRORで出る（500は400以上でもあるので、WARNになっていないことの確認にもなる）
    assertThat(output).contains("POST /api/deliveries 500");
  }

  @Test
  void api以外のURLはログを出さない(CapturedOutput output) throws Exception {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui.html");// /apiで始まらないURLにする。このテストだけはURLの違いに意味がある
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(200);

    // When
    apiAccessLogFilter.doFilter(request, response, chain);

    // Then
    assertThat(output).doesNotContain("GET /swagger-ui.html 200");// doesNotContainは「含まれていない」ことを確かめる。shouldNotFilterで素通りしたので、ログは出ない
  }

  @Test
  void 処理中はリクエストIDがあり終わったら消える() throws Exception {// ログの中身は見ないので、CapturedOutputは使わない
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> idInRequest = new AtomicReference<>();// 値を1つ入れられる箱。ラムダの中からは普通の変数に代入できないので、箱の中身を入れ替える
    FilterChain chain = (req, res) -> idInRequest.set(MDC.get("requestId"));// ラムダの中＝処理中。そのときのリクエストIDをMDCから取り出して箱に入れる

    // When
    apiAccessLogFilter.doFilter(request, response, chain);

    // Then
    assertThat(idInRequest.get()).isNotNull();// 処理中はIDが入っていた（ログにreq=が付く）
    assertThat(MDC.get("requestId")).isNull();// 処理が終わったらIDが消えている（次のリクエストに前のIDが残らない）
  }

  @Test
  void 例外が起きたときにリクエストIDが消える() throws Exception {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> {
      throw new RuntimeException("テスト用のエラー");
    };

    // When / Then
    assertThatThrownBy(() -> apiAccessLogFilter.doFilter(request, response, chain))
        .isInstanceOf(RuntimeException.class);
    assertThat(MDC.get("requestId")).isNull();
  }
}
