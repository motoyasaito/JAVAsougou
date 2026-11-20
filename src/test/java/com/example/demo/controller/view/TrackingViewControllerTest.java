package com.example.demo.controller.view;

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

import com.example.demo.dto.DeliveryStatusResponse;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.DeliveryService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrackingViewController.class)
class TrackingViewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DeliveryService deliveryService;

  @Test
  void 入力画面を表示する() throws Exception {
    mockMvc.perform(get("/tracking"))
        .andExpect(status().isOk())
        .andExpect(view().name("customer/tracking"));
  }

  @Test
  void 見つかれば配送状況の画面に状況を渡して表示する() throws Exception {
    DeliveryStatusResponse delivery = new DeliveryStatusResponse("100000000005", LocalDate.of(2026, 10, 21),
        "12:00-14:00", "ABSENT");
    when(deliveryService.findStatusByTrackingNumber("100000000005")).thenReturn(delivery);

    mockMvc.perform(get("/tracking/status").param("trackingNumber", "100000000005"))
        .andExpect(status().isOk())
        .andExpect(view().name("customer/trackingStatus"))
        .andExpect(model().attribute("delivery", delivery));
  }

  @Test
  void 前後の空白は取ってから探す() throws Exception {
    when(deliveryService.findStatusByTrackingNumber("100000000005")).thenReturn(
        new DeliveryStatusResponse("100000000005", LocalDate.of(2026, 10, 21), "12:00-14:00", "ABSENT"));

    mockMvc.perform(get("/tracking/status").param("trackingNumber", " 100000000005 "))
        .andExpect(view().name("customer/trackingStatus"));
  }

  @Test
  void 見つからなければ入力画面に戻してメッセージと入力した値を渡す() throws Exception {
    when(deliveryService.findStatusByTrackingNumber("999999999999"))
        .thenThrow(new NotFoundException("該当する配送物が見つかりません"));

    mockMvc.perform(get("/tracking/status").param("trackingNumber", "999999999999"))
        .andExpect(redirectedUrl("/tracking"))
        .andExpect(flash().attribute("errorMessage", "該当する配送物が見つかりません"))
        .andExpect(flash().attribute("trackingNumber", "999999999999"));
  }

  @Test
  void 十二桁の数字でなければDBを探さずに入力画面に戻す() throws Exception {
    mockMvc.perform(get("/tracking/status").param("trackingNumber", "12345"))
        .andExpect(redirectedUrl("/tracking"))
        .andExpect(flash().attribute("errorMessage", "配送番号は12桁の数字で入力してください"));

    verify(deliveryService, never()).findStatusByTrackingNumber(anyString());
  }

  @Test
  void 想定外のエラーならお客さん用の外枠でエラー画面を出す() throws Exception {
    when(deliveryService.findStatusByTrackingNumber("100000000005")).thenThrow(new RuntimeException("DBにつながらない"));

    mockMvc.perform(get("/tracking/status").param("trackingNumber", "100000000005"))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("common/error"))
        .andExpect(model().attribute("adminPage", false));// 管理者のメニューを出さない
  }
}
