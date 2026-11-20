package com.example.demo.controller.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.demo.dto.AssignDriverRequest;
import com.example.demo.dto.DeliveryDetailResponse;
import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.DriverOptionResponse;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.DeliveryService;
import com.example.demo.service.DriverService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminDeliveryDetailViewController.class)
class AdminDeliveryDetailViewControllerTest {

  private static final String TRACKING_NUMBER = "100000000001";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DeliveryService deliveryService;

  @MockitoBean
  private DriverService driverService;

  // テストで使う配送物（id=1、発送準備中、未割り当て）
  private DeliveryResponse delivery() {
    return new DeliveryResponse(1L, TRACKING_NUMBER, "山田 花子", "埼玉県さいたま市浦和区1-1-1",
        "08022220001", "yamada@example.com", "PREPARING", "NORMAL", null, LocalDate.of(2026, 10, 23), "午前中");
  }

  // ---------- 詳細の表示 ----------

  @Test
  void 詳細_配送物と登録日時とドライバー一覧を渡して詳細のJSPを表示する() throws Exception {
    DeliveryResponse delivery = delivery();
    DeliveryDetailResponse detail = new DeliveryDetailResponse(delivery,
        LocalDateTime.of(2026, 10, 20, 10, 15), LocalDateTime.of(2026, 10, 21, 9, 0));
    List<DriverOptionResponse> drivers = List.of(new DriverOptionResponse(2L, "配送 一郎", "埼玉"));
    when(deliveryService.findDetailByTrackingNumber(TRACKING_NUMBER)).thenReturn(detail);
    when(driverService.findDrivers()).thenReturn(drivers);

    mockMvc.perform(get("/admin/deliveries/" + TRACKING_NUMBER))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/deliveryDetail"))
        .andExpect(model().attribute("delivery", delivery))// いつもの項目は "delivery" で渡す
        .andExpect(model().attribute("detail", detail))// 登録日時・最終更新日時は "detail" で渡す
        .andExpect(model().attribute("drivers", drivers));
  }

  @Test
  void 詳細_見つからなければ404でエラー画面を表示する() throws Exception {
    when(deliveryService.findDetailByTrackingNumber("999999999999"))
        .thenThrow(new NotFoundException("該当する配送物が見つかりません"));

    mockMvc.perform(get("/admin/deliveries/999999999999"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("common/error"))
        .andExpect(model().attribute("message", "該当する配送物が見つかりません"));
  }

  @Test
  void 詳細_伝票番号の形が違えばDBを探さずに404にする() throws Exception {
    mockMvc.perform(get("/admin/deliveries/abc"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("common/error"));

    verify(deliveryService, never()).findDetailByTrackingNumber(anyString());
  }

  // ---------- ドライバーの割り当て ----------

  @Test
  void 割り当て_成功したら詳細画面に戻って完了メッセージを出す() throws Exception {
    when(deliveryService.findByTrackingNumber(TRACKING_NUMBER)).thenReturn(delivery());

    mockMvc.perform(post("/admin/deliveries/" + TRACKING_NUMBER + "/driver").param("driverId", "2"))
        .andExpect(redirectedUrl("/admin/deliveries/" + TRACKING_NUMBER))
        .andExpect(flash().attribute("successMessage", "ドライバーを割り当てました"));

    verify(deliveryService).assignDriver(1L, new AssignDriverRequest(2L));// 配送物の id と選んだドライバーで、APIと同じServiceを呼ぶ
  }

  @Test
  void 割り当て_ドライバーを選ばずに押すとメッセージを出して割り当てない() throws Exception {
    when(deliveryService.findByTrackingNumber(TRACKING_NUMBER)).thenReturn(delivery());

    mockMvc.perform(post("/admin/deliveries/" + TRACKING_NUMBER + "/driver"))
        .andExpect(redirectedUrl("/admin/deliveries/" + TRACKING_NUMBER))
        .andExpect(flash().attribute("errorMessage", "ドライバーを選択してください"));

    verify(deliveryService, never()).assignDriver(anyLong(), any());
  }

  @Test
  void 割り当て_発送準備中でなければServiceのメッセージを出す() throws Exception {
    when(deliveryService.findByTrackingNumber(TRACKING_NUMBER)).thenReturn(delivery());
    doThrow(new ConflictException("発送準備中の配送物のみ担当を割り振れます"))
        .when(deliveryService).assignDriver(eq(1L), any());

    mockMvc.perform(post("/admin/deliveries/" + TRACKING_NUMBER + "/driver").param("driverId", "2"))
        .andExpect(redirectedUrl("/admin/deliveries/" + TRACKING_NUMBER))
        .andExpect(flash().attribute("errorMessage", "発送準備中の配送物のみ担当を割り振れます"));
  }

  @Test
  void 割り当て_ドライバーがいなければServiceのメッセージを出す() throws Exception {
    when(deliveryService.findByTrackingNumber(TRACKING_NUMBER)).thenReturn(delivery());
    doThrow(new NotFoundException("該当するドライバーが見つかりません"))
        .when(deliveryService).assignDriver(eq(1L), any());

    mockMvc.perform(post("/admin/deliveries/" + TRACKING_NUMBER + "/driver").param("driverId", "99"))
        .andExpect(redirectedUrl("/admin/deliveries/" + TRACKING_NUMBER))
        .andExpect(flash().attribute("errorMessage", "該当するドライバーが見つかりません"));
  }

  @Test
  void 割り当て_配送物が見つからなければ404でエラー画面を表示する() throws Exception {
    when(deliveryService.findByTrackingNumber("999999999999"))
        .thenThrow(new NotFoundException("該当する配送物が見つかりません"));

    mockMvc.perform(post("/admin/deliveries/999999999999/driver").param("driverId", "2"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("common/error"));
  }
}
