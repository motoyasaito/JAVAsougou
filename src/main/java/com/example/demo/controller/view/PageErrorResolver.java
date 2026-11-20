package com.example.demo.controller.view;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 画面の URL で「Controller が見つからない」エラーが起きたときに、JSON ではなくエラー画面を返す振り分け係
//
// 存在しない URL（404）や、使えないメソッド（405）のエラーは、どの Controller にも届く前に起きる。
// そのため、画面の Controller だけを受け持つ ViewExceptionHandler では受け取れず、
// これまでは API 用の GlobalExceptionHandler が JSON を返していた（ブラウザに JSON の文字が出てしまう）。
//
// HandlerExceptionResolver は、エラーを受け取ったときに「自分は担当しない」と null を返せる。
// /api の URL のときは null を返して、今までどおり GlobalExceptionHandler に JSON を返してもらう。
@Component // Spring に登録すると、エラーが起きたときに自動で呼ばれる
public class PageErrorResolver implements HandlerExceptionResolver, Ordered {

  @Override
  public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
      Exception ex) {
    String uri = request.getRequestURI();
    if (uri.startsWith("/api")) {
      return null;// API は担当しない。GlobalExceptionHandler が JSON を返す
    }

    if (ex instanceof NoResourceFoundException) {// instanceof：「この例外の種類か」を調べる
      return errorPage(response, uri, HttpStatus.NOT_FOUND, "お探しのページは見つかりませんでした。URLをご確認ください");
    }
    if (ex instanceof HttpRequestMethodNotSupportedException) {
      return errorPage(response, uri, HttpStatus.METHOD_NOT_ALLOWED, "この操作は受け付けられません。画面のボタンから操作してください");
    }
    return null;// それ以外のエラーは担当しない（画面の Controller のエラーは ViewExceptionHandler が受け持つ）
  }

  // エラー画面（/WEB-INF/jsp/common/error.jsp）を返す
  private ModelAndView errorPage(HttpServletResponse response, String uri, HttpStatus status, String message) {
    response.setStatus(status.value());// 画面を返すときも、ステータスコードは 404・405 にする
    ModelAndView modelAndView = new ModelAndView("common/error");// ModelAndView：表示する JSP の名前と、JSP に渡す値をまとめたもの
    modelAndView.addObject("message", message);
    modelAndView.addObject("area", ErrorPageArea.from(uri));// 外枠（管理者・ドライバー・お客さん）を決める
    return modelAndView;
  }

  // ほかの係より先に呼ばれるようにする（数字が小さいほど先）。ここで null を返せば、次の係に回る
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
