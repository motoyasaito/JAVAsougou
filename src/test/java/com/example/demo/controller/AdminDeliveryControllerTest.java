package com.example.demo.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

// Service をモックにして、パラメータの受け取り方・ステータス・JSON の形を確認する
@WebMvcTest(AdminDeliveryController.class) // 指定したコントローラーだけで、springのweb部分を起動。DBやマッパーは起動しない。
class AdminDeliveryControllerTest {

  @Autowired
  MockMvc mockMvc;// MockMvcはテスト用のPostマン。実際にサーバーを立てずにコントローラーにリクエストを送れる。

  @MockitoBean
  DeliveryService deliveryService;// サービスの偽物を作ってコントローラーに返す。

  @Test
  void ページと件数を指定すると配送物の一覧を返す() throws Exception {
    // Given 偽物サービスに「findAll(1．5)と呼ばれたら1件入った結果を返す（全6件）」と教える
    DeliveryResponse delivery = new DeliveryResponse(1L, "123456789012", "山田太郎", "東京都千代田区1-1",
        "09012345678", "taro@example.com", "PREPARING", "NORMAL", 2L, LocalDate.of(2026, 10, 23), "午前中");
    when(deliveryService.findAll(1, 5)).thenReturn(PageResponse.of(List.of(delivery), 1, 5, 6L));

    // When / Then
    // When: GET /api/admin/deliveries?page=1&size=5 を送ります。Postman の Params
    // 欄に入力するのと同じです。
    // ここで大事なのは、コントローラーが URL の "1" と "5"（文字）を int の 1 と 5 に変えて、正しい順番でサービスに渡せるか。もし
    // page と size を逆に渡していたら、偽物は何も返さず、下の確認で失敗する。
    mockMvc.perform(get("/api/admin/deliveries")
        .param("page", "1")
        .param("size", "5"))
        .andExpect(status().isOk())// ステータスが200か確かめる
        .andExpect(jsonPath("$.content[0].trackingNumber").value("123456789012"))// 返ってきたJsonの中身を確かめる。$ が JSON
                                                                                 // 全体で、.content[0] は「content
                                                                                 // の1件目」、.trackingNumber はその中の項目
        .andExpect(jsonPath("$.content[0].deliveryDate").value("2026-10-23"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(6))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void ページ指定を省略したら既定値でServiceを呼ぶ() throws Exception {
    // Given
    when(deliveryService.findAll(0, 10)).thenReturn(PageResponse.of(List.of(), 0, 10, 0L));

    // When パラメータなしで送ったときに、defaultValue の 0 と 10 でサービスが呼ばれるかを確かめる。もし誰かが defaultValue
    // を書き換えたら、ここで気づける。
    mockMvc.perform(get("/api/admin/deliveries"))
        .andExpect(status().isOk());

    // Then
    verify(deliveryService).findAll(0, 10);
  }

  @Test
  void sizeが上限の100なら取得できる() throws Exception {
    // Given ＠Max（100）は100以下ならOKという意味。ちょうど100が通るかを確かめる。
    when(deliveryService.findAll(0, 100)).thenReturn(PageResponse.of(List.of(), 0, 100, 0L));

    // When / Then
    mockMvc.perform(get("/api/admin/deliveries").param("size", "100"))
        .andExpect(status().isOk());
  }

  @Test
  // isBadRequest() は「400 か」を確かめる。ここではモックに何も教えていない。入力チェックで止まるので、サービスまで届かないから。
  void sizeが0なら400を返す() throws Exception {
    mockMvc.perform(get("/api/admin/deliveries").param("size", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sizeが101なら400を返す() throws Exception {
    mockMvc.perform(get("/api/admin/deliveries").param("size", "101"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void pageが負の数なら400を返す() throws Exception {
    mockMvc.perform(get("/api/admin/deliveries").param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sizeが数字でなければ400を返す() throws Exception {
    mockMvc.perform(get("/api/admin/deliveries").param("size", "abc"))
        .andExpect(status().isBadRequest());
  }
}
