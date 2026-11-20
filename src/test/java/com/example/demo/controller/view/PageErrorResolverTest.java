package com.example.demo.controller.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 振り分け係（PageErrorResolver）のテスト。Spring を起動せず、メソッドを直接呼んで確かめる
class PageErrorResolverTest {

  private final PageErrorResolver resolver = new PageErrorResolver();

  // 指定した URL へのリクエストで、指定した例外が起きたことにして、振り分け係を呼ぶ
  private ModelAndView resolve(String uri, Exception ex, MockHttpServletResponse response) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);// テスト用の偽のリクエスト
    return resolver.resolveException(request, response, null, ex);
  }

  @Test
  void 画面のURLがなければ404でエラー画面を返す() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    ModelAndView result = resolve("/admin/xyz", new NoResourceFoundException(HttpMethod.GET, "/admin/xyz", null), response);

    assertThat(result.getViewName()).isEqualTo("common/error");
    assertThat(result.getModel()).containsEntry("message", "お探しのページは見つかりませんでした。URLをご確認ください")
        .containsEntry("area", "admin");
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void 使えないメソッドなら405でエラー画面を返す() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    ModelAndView result = resolve("/logout", new HttpRequestMethodNotSupportedException("GET"), response);

    assertThat(result.getViewName()).isEqualTo("common/error");
    assertThat(result.getModel()).containsEntry("area", "public");
    assertThat(response.getStatus()).isEqualTo(405);
  }

  @Test
  void APIのURLなら担当せずにGlobalExceptionHandlerへ回す() {
    ModelAndView result = resolve("/api/xyz", new NoResourceFoundException(HttpMethod.GET, "/api/xyz", null),
        new MockHttpServletResponse());

    assertThat(result).isNull();// null は「担当しない」。今までどおり JSON が返る
  }

  @Test
  void 画面のURLでもほかの例外は担当しない() {
    ModelAndView result = resolve("/admin/deliveries", new IllegalStateException("想定外"), new MockHttpServletResponse());

    assertThat(result).isNull();// 画面の Controller の中のエラーは ViewExceptionHandler が受け持つ
  }
}
