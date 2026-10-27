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
@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  DeliveryService deliveryService;

  @Test
  void ドライバーIDと日付を指定すると配達一覧を返す() throws Exception {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);
    DeliveryResponse delivery = new DeliveryResponse(1L, "123456789012", "山田太郎", "東京都千代田区1-1",
        "09012345678", "taro@example.com", "IN TRANSIT", "NORMAL", 1L, date, "14:00-16:00");
    when(deliveryService.findByDriver(1L, date, 0, 10))
        .thenReturn(PageResponse.of(List.of(delivery), 0, 10, 1L));

    // When / Then
    mockMvc.perform(get("/api/deliveries")
        .param("driverId", "1")
        .param("date", "2026-10-26"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].trackingNumber").value("123456789012"))
        .andExpect(jsonPath("$.content[0].deliveryDate").value("2026-10-26"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void 日付とページ指定を省略したら既定値でServiceを呼ぶ() throws Exception {
    // Given
    when(deliveryService.findByDriver(1L, null, 0, 10))
        .thenReturn(PageResponse.of(List.of(), 0, 10, 0L));

    // When
    mockMvc.perform(get("/api/deliveries").param("driverId", "1"))
        .andExpect(status().isOk());

    // Then
    verify(deliveryService).findByDriver(1L, null, 0, 10);
  }

  @Test
  void sizeが上限の100なら取得できる() throws Exception {
    // Given
    when(deliveryService.findByDriver(1L, null, 0, 100))
        .thenReturn(PageResponse.of(List.of(), 0, 100, 0L));

    // When / Then
    mockMvc.perform(get("/api/deliveries")
        .param("driverId", "1")
        .param("size", "100"))
        .andExpect(status().isOk());
  }

  @Test
  void ドライバーIDがなければ400を返す() throws Exception {
    mockMvc.perform(get("/api/deliveries"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void ドライバーIDが数字でなければ400を返す() throws Exception {
    mockMvc.perform(get("/api/deliveries").param("driverId", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void ドライバーIDが0の場合400を返す() throws Exception {
    mockMvc.perform(get("/api/deliveries").param("driverId", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void 日付の形式が正しくなければ400を返す() throws Exception {
    mockMvc.perform(get("/api/deliveries")
        .param("driverId", "1")
        .param("date", "2026/10/26"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void sizeが0なら400を返す() throws Exception {
    mockMvc.perform(get("/api/deliveries")
        .param("driverId", "1")
        .param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void sizeが101なら400を返す() throws Exception {
    mockMvc.perform(get("/api/deliveries")
        .param("driverId", "1")
        .param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void pageが負の数なら400を返す() throws Exception {
    mockMvc.perform(get("/api/deliveries")
        .param("driverId", "1")
        .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
