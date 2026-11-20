package com.example.demo.controller.view;

import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.any;
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

import com.example.demo.dto.CreateDeliveryRequest;
import com.example.demo.dto.DeliveryResponse;
import com.example.demo.service.DeliveryService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminDeliveryNewViewController.class)
class AdminDeliveryNewViewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DeliveryService deliveryService;

  // 正しく入力した登録フォームを作る。changes に入れた項目だけ、値を差し替える
  // 注意：.param() は同じ名前で2回呼ぶと「上書き」ではなく「値が2つ」になるので、先に Map で差し替えてから入れる
  private MockHttpServletRequestBuilder form(Map<String, String> changes) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("name", "山田 花子");
    values.put("callNumber", "08022220001");
    values.put("email", "yamada@example.com");
    values.put("address", "埼玉県さいたま市浦和区1-1-1");
    values.put("itemType", "NORMAL");
    values.put("deliveryDate", "2026-11-21");
    values.put("deliveryTime", "午前中");
    values.putAll(changes);// 同じキーなら上書きされる

    MockHttpServletRequestBuilder request = post("/admin/deliveries");
    values.forEach(request::param);// 1項目ずつ param に入れる
    return request;
  }

  private MockHttpServletRequestBuilder validForm() {
    return form(Map.of());
  }

  @Test
  void 登録画面を開くと登録のJSPを表示する() throws Exception {
    mockMvc.perform(get("/admin/deliveries/new"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/deliveryNew"));
  }

  @Test
  void 正しく入力すると登録して詳細画面へ移り完了メッセージを出す() throws Exception {
    DeliveryResponse created = new DeliveryResponse(6L, "123456789012", "山田 花子", "埼玉県さいたま市浦和区1-1-1",
        "08022220001", "yamada@example.com", "PREPARING", "NORMAL", null, LocalDate.of(2026, 11, 21), "午前中");
    when(deliveryService.create(any())).thenReturn(created);

    mockMvc.perform(validForm())
        .andExpect(redirectedUrl("/admin/deliveries/123456789012"))
        .andExpect(flash().attribute("successMessage", "荷物を登録しました（伝票番号：123456789012）"));

    // フォームの値が、APIと同じ CreateDeliveryRequest に入って Service に渡っている
    verify(deliveryService).create(new CreateDeliveryRequest("山田 花子", "埼玉県さいたま市浦和区1-1-1",
        "08022220001", "yamada@example.com", "NORMAL", LocalDate.of(2026, 11, 21), "午前中"));
  }

  @Test
  void 必須の項目が空なら登録せずに登録画面を出して項目ごとのメッセージを渡す() throws Exception {
    mockMvc.perform(form(Map.of("name", "", "deliveryDate", "")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/deliveryNew"))
        .andExpect(model().attribute("fieldErrors", hasEntry("name", "お届け先の氏名を入力してください")))
        .andExpect(model().attribute("fieldErrors", hasEntry("deliveryDate", "配達日を入力してください")));

    verify(deliveryService, never()).create(any());
  }

  @Test
  void 電話番号の形が違えばメッセージを出す() throws Exception {
    mockMvc.perform(form(Map.of("callNumber", "090-1111-0000")))
        .andExpect(view().name("admin/deliveryNew"))
        .andExpect(model().attribute("fieldErrors",
            hasEntry("callNumber", "電話番号はハイフンなしの10桁または11桁の数字で入力してください")));

    verify(deliveryService, never()).create(any());
  }

  @Test
  void 日付の形が違えば形式のメッセージを出す() throws Exception {
    mockMvc.perform(form(Map.of("deliveryDate", "abc")))
        .andExpect(view().name("admin/deliveryNew"))
        .andExpect(model().attribute("fieldErrors", hasEntry("deliveryDate", "形式が正しくありません")));

    verify(deliveryService, never()).create(any());
  }
}
