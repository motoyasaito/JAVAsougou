package com.example.demo.controller.view;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.DeliveryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 画面のControllerのテスト。JSPの中身（HTML）までは作らず、「どのJSPを表示するか」と「JSPに何を渡すか」を確かめる
@WebMvcTest(AdminDeliveryViewController.class)
class AdminDeliveryViewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DeliveryService deliveryService;

  @Test
  void 一覧画面を開くと1ページ目を10件ずつで取得して一覧のJSPを表示する() throws Exception {
    DeliveryResponse delivery = new DeliveryResponse(1L, "100000000001", "山田 花子", "埼玉県さいたま市浦和区1-1-1",
        "08022220001", "yamada@example.com", "PREPARING", "NORMAL", 2L, LocalDate.of(2026, 10, 23), "午前中");
    PageResponse<DeliveryResponse> result = PageResponse.of(List.of(delivery), 0, 10, 1);
    when(deliveryService.findAll(0, 10)).thenReturn(result);

    mockMvc.perform(get("/admin/deliveries"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/deliveries"))// 表示するJSPの名前
        .andExpect(model().attribute("result", result));// JSPに "result" という名前で一覧を渡している
  }

  @Test
  void pageを指定するとそのページを取得する() throws Exception {
    when(deliveryService.findAll(anyInt(), anyInt())).thenReturn(PageResponse.of(List.of(), 2, 10, 25));

    mockMvc.perform(get("/admin/deliveries").param("page", "2"))
        .andExpect(status().isOk());

    verify(deliveryService).findAll(2, 10);
  }

  @Test
  void pageがマイナスなら1ページ目を表示する() throws Exception {
    when(deliveryService.findAll(anyInt(), anyInt())).thenReturn(PageResponse.of(List.of(), 0, 10, 0));

    mockMvc.perform(get("/admin/deliveries").param("page", "-1"))
        .andExpect(status().isOk());

    verify(deliveryService).findAll(0, 10);// 400にせず、0（1ページ目）に直して取得している
  }

  @Test
  void pageが数字でなければ400でエラー画面を表示する() throws Exception {
    mockMvc.perform(get("/admin/deliveries").param("page", "a"))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("common/error"))// JSONではなくエラー画面になる（ViewExceptionHandler）
        .andExpect(model().attribute("message", "URLの指定が正しくありません"));
  }

  // ---------- 伝票番号の検索（/admin/deliveries/search） ----------

  @Test
  void 検索_見つかればその配送物の詳細画面へ移る() throws Exception {
    when(deliveryService.existsByTrackingNumber("100000000001")).thenReturn(true);

    mockMvc.perform(get("/admin/deliveries/search").param("trackingNumber", "100000000001"))
        .andExpect(status().is3xxRedirection())// 画面を表示せず、別のURLを開き直させる（リダイレクト）
        .andExpect(redirectedUrl("/admin/deliveries/100000000001"));
  }

  @Test
  void 検索_前後の空白は取ってから探す() throws Exception {
    when(deliveryService.existsByTrackingNumber("100000000001")).thenReturn(true);

    mockMvc.perform(get("/admin/deliveries/search").param("trackingNumber", " 100000000001 "))
        .andExpect(redirectedUrl("/admin/deliveries/100000000001"));
  }

  @Test
  void 検索_見つからなければ一覧に戻してメッセージを出す() throws Exception {
    when(deliveryService.existsByTrackingNumber("999999999999")).thenReturn(false);

    mockMvc.perform(get("/admin/deliveries/search").param("trackingNumber", "999999999999"))
        .andExpect(redirectedUrl("/admin/deliveries"))
        .andExpect(flash().attribute("searchError", "該当する配送物が見つかりません"))// 移った先で1回だけ使える値
        .andExpect(flash().attribute("trackingNumber", "999999999999"));// 入力した値を検索欄に戻す
  }

  @Test
  void 検索_12桁の数字でなければDBを探さずにメッセージを出す() throws Exception {
    mockMvc.perform(get("/admin/deliveries/search").param("trackingNumber", "12345"))
        .andExpect(redirectedUrl("/admin/deliveries"))
        .andExpect(flash().attribute("searchError", "伝票番号は12桁の数字で入力してください"));

    verify(deliveryService, never()).existsByTrackingNumber(anyString());// 形が違う時点で止めて、DBは探さない
  }

  @Test
  void 検索_空のまま検索するとメッセージを出す() throws Exception {
    mockMvc.perform(get("/admin/deliveries/search"))
        .andExpect(redirectedUrl("/admin/deliveries"))
        .andExpect(flash().attribute("searchError", "伝票番号は12桁の数字で入力してください"));
  }

  // ---------- エラー画面 ----------

  @Test
  void 想定外のエラーなら500でエラー画面を表示する() throws Exception {
    when(deliveryService.findAll(anyInt(), anyInt())).thenThrow(new RuntimeException("DBにつながらない"));

    mockMvc.perform(get("/admin/deliveries"))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("common/error"))
        .andExpect(model().attribute("message", "システムでエラーが発生しました。時間をおいて、もう一度お試しください"));
  }
}
