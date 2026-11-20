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

import com.example.demo.dto.CreateRedeliveryRequest;
import com.example.demo.dto.RedeliveryResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.RedeliveryService;
import java.time.LocalDate;
import com.example.demo.support.FixedClockConfig;
import org.springframework.context.annotation.Import;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(RedeliveryRequestViewController.class)
@Import(FixedClockConfig.class) // 本物の時計の代わりに、決まった日の時計を使う
class RedeliveryRequestViewControllerTest {

  // 明日（決まった日 2026-11-20 の次の日）。希望日は明日以降しか選べないので、テストでも明日を使う
  private static final LocalDate TOMORROW = FixedClockConfig.TODAY.plusDays(1);

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RedeliveryService redeliveryService;

  // 正しく入力した依頼フォームを作る。changes に入れた項目だけ、値を差し替える
  private MockHttpServletRequestBuilder form(Map<String, String> changes) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("trackingNumber", "100000000005");
    values.put("callNumber", "08022220005");
    values.put("redeliveryDate", TOMORROW.toString());
    values.put("redeliveryTime", "18:00-20:00");
    values.putAll(changes);

    MockHttpServletRequestBuilder request = post("/redelivery");
    values.forEach(request::param);
    return request;
  }

  @Test
  void 入力画面を表示して選べるいちばん早い日として明日を渡す() throws Exception {
    mockMvc.perform(get("/redelivery").param("trackingNumber", "100000000005"))
        .andExpect(status().isOk())
        .andExpect(view().name("customer/redelivery"))
        .andExpect(model().attribute("minDate", TOMORROW));
  }

  @Test
  void 受け付けたら入力画面に戻して完了の内容を渡す() throws Exception {
    RedeliveryResponse created = new RedeliveryResponse("100000000005", TOMORROW, "18:00-20:00");
    when(redeliveryService.create(any())).thenReturn(created);

    mockMvc.perform(form(Map.of()))
        .andExpect(redirectedUrl("/redelivery"))
        .andExpect(flash().attribute("completed", created));// 完了のモーダルに出す内容

    // フォームの値が、APIと同じ CreateRedeliveryRequest に入って Service に渡っている
    verify(redeliveryService).create(
        new CreateRedeliveryRequest("100000000005", "08022220005", TOMORROW, "18:00-20:00"));
  }

  @Test
  void 入力に間違いがあれば受け付けずに項目ごとのメッセージを渡す() throws Exception {
    mockMvc.perform(form(Map.of("callNumber", "", "redeliveryTime", "")))
        .andExpect(status().isOk())
        .andExpect(view().name("customer/redelivery"))
        .andExpect(model().attribute("fieldErrors", hasEntry("callNumber", "電話番号を入力してください")))
        .andExpect(model().attribute("fieldErrors", hasEntry("redeliveryTime", "時間帯を入力してください")))
        .andExpect(model().attribute("minDate", TOMORROW));// もう一度表示するときも、カレンダーの制限を付ける

    verify(redeliveryService, never()).create(any());
  }

  @Test
  void 番号と電話番号が合わなければServiceのメッセージを出す() throws Exception {
    when(redeliveryService.create(any())).thenThrow(new NotFoundException("該当する配送物が見つかりません"));

    mockMvc.perform(form(Map.of()))
        .andExpect(view().name("customer/redelivery"))
        .andExpect(model().attribute("errorMessage", "該当する配送物が見つかりません"));
  }

  @Test
  void 明日より前の日付ならServiceのメッセージを出す() throws Exception {
    when(redeliveryService.create(any())).thenThrow(new BadRequestException("再配達日は明日以降の日付を選択してください"));

    mockMvc.perform(form(Map.of()))
        .andExpect(view().name("customer/redelivery"))
        .andExpect(model().attribute("errorMessage", "再配達日は明日以降の日付を選択してください"));
  }

  @Test
  void 不在でなければServiceのメッセージを出す() throws Exception {
    when(redeliveryService.create(any())).thenThrow(new ConflictException("不在の配送物のみ再配達を受け付けられます"));

    mockMvc.perform(form(Map.of()))
        .andExpect(view().name("customer/redelivery"))
        .andExpect(model().attribute("errorMessage", "不在の配送物のみ再配達を受け付けられます"));
  }
}
