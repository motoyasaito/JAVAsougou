package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.AssignDriverRequest;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.RedeliveryService;

import org.springframework.http.MediaType;

@WebMvcTest(AdminRedeliveryController.class)
class AdminRedeliveryControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  RedeliveryService redeliveryService;

  @Test
  void 割り振れたら200を返す() throws Exception {
    mockMvc.perform(post("/api/admin/redeliveries/1/driver")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"driverId\":2}"))
        .andExpect(status().isOk());

    verify(redeliveryService).reassignDriver(1L, new AssignDriverRequest(2L));
  }

  @Test
  void driverIDがなければ400を返す() throws Exception {
    mockMvc.perform(post("/api/admin/redeliveries/1/driver")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0]").value("driverId:ドライバーを選択してください"));

    verify(redeliveryService, never()).reassignDriver(anyLong(), any());
  }

  @Test
  void driverIDが文字列だった場合400を返す() throws Exception {
    mockMvc.perform(post("/api/admin/redeliveries/1/driver")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"driverId\":\"abcde\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("リクエストの形式が正しくありません"));

    verify(redeliveryService, never()).reassignDriver(anyLong(), any());
  }

  @Test
  void 配送物が見つからなければ404を返す() throws Exception {
    // Given
    doThrow(new NotFoundException("該当する配送物が見つかりません"))
        .when(redeliveryService).reassignDriver(anyLong(), any());

    // When/Then
    mockMvc.perform(post("/api/admin/redeliveries/1/driver")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"driverId\":2}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("該当する配送物が見つかりません"));
  }

  @Test
  void 発送準備中でなければ409を返す() throws Exception {
    // Given
    doThrow(new ConflictException("発送準備中の配送物のみ担当を割り振れます"))
        .when(redeliveryService).reassignDriver(anyLong(), any());

    // When/Then
    mockMvc.perform(post("/api/admin/redeliveries/1/driver")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"driverId\":2}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("発送準備中の配送物のみ担当を割り振れます"));
  }
}
