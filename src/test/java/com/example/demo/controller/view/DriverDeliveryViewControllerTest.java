package com.example.demo.controller.view;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.DriverOptionResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.DeliveryService;
import com.example.demo.service.DriverService;
import java.time.LocalDate;
import com.example.demo.support.FixedClockConfig;
import org.springframework.context.annotation.Import;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DriverDeliveryViewController.class)
@Import(FixedClockConfig.class) // 本物の時計の代わりに、決まった日の時計を使う
class DriverDeliveryViewControllerTest {

  private static final LocalDate TODAY = FixedClockConfig.TODAY;// 決まった日（2026-11-20）。FixedClockConfig の時計と同じ日

  private static final DriverOptionResponse ICHIRO = new DriverOptionResponse(2L, "配送 一郎", "埼玉");
  private static final DriverOptionResponse JIRO = new DriverOptionResponse(3L, "配送 二郎", "東京");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DeliveryService deliveryService;

  @MockitoBean
  private DriverService driverService;

  @BeforeEach // 各テストの前に毎回実行する。どのテストでもドライバーは2人いる状態にする
  void setUp() {
    when(driverService.findDrivers()).thenReturn(List.of(ICHIRO, JIRO));
  }

  private DeliveryResponse delivery(long driverId) {
    return new DeliveryResponse(1L, "100000000001", "山田 花子", "埼玉県さいたま市浦和区1-1-1", "08022220001",
        "yamada@example.com", "IN TRANSIT", "NORMAL", driverId, TODAY, "午前中");
  }

  @Test
  void ドライバーを指定すると今日のその人の配達物を出す() throws Exception {
    List<DeliveryResponse> deliveries = List.of(delivery(2L));
    when(deliveryService.findByDriver(2L, TODAY, 0, 100)).thenReturn(PageResponse.of(deliveries, 0, 100, 1));

    mockMvc.perform(get("/driver/deliveries").param("driverId", "2"))
        .andExpect(status().isOk())
        .andExpect(view().name("driver/deliveries"))
        .andExpect(model().attribute("driver", ICHIRO))
        .andExpect(model().attribute("date", TODAY))
        .andExpect(model().attribute("deliveries", deliveries))
        .andExpect(model().attribute("total", 1L));
  }

  @Test
  void 別のドライバーを指定するとその人の分を取る() throws Exception {
    when(deliveryService.findByDriver(anyLong(), any(), anyInt(), anyInt())).thenReturn(PageResponse.of(List.of(), 0, 100, 0));

    mockMvc.perform(get("/driver/deliveries").param("driverId", "3"))
        .andExpect(model().attribute("driver", JIRO));

    verify(deliveryService).findByDriver(3L, TODAY, 0, 100);
  }

  @Test
  void 日付を指定するとその日の分を取る() throws Exception {
    LocalDate tomorrow = TODAY.plusDays(1);
    when(deliveryService.findByDriver(anyLong(), any(), anyInt(), anyInt())).thenReturn(PageResponse.of(List.of(), 0, 100, 0));

    mockMvc.perform(get("/driver/deliveries").param("driverId", "2").param("date", tomorrow.toString()))
        .andExpect(model().attribute("date", tomorrow));

    verify(deliveryService).findByDriver(2L, tomorrow, 0, 100);
  }

  @Test
  void ドライバーを指定しなければ一覧を取らずに選んでもらう() throws Exception {
    mockMvc.perform(get("/driver/deliveries"))
        .andExpect(status().isOk())
        .andExpect(view().name("driver/deliveries"))
        .andExpect(model().attributeDoesNotExist("driver"))
        .andExpect(model().attribute("drivers", List.of(ICHIRO, JIRO)));

    verify(deliveryService, never()).findByDriver(anyLong(), any(), anyInt(), anyInt());
  }

  @Test
  void ドライバーでないIDなら一覧を取らずに選んでもらう() throws Exception {
    mockMvc.perform(get("/driver/deliveries").param("driverId", "1"))// id=1 は管理者
        .andExpect(model().attributeDoesNotExist("driver"));

    verify(deliveryService, never()).findByDriver(anyLong(), any(), anyInt(), anyInt());
  }
}
