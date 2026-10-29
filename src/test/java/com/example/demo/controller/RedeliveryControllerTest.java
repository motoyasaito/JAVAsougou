package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.dto.CreateRedeliveryRequest;
import com.example.demo.dto.RedeliveryResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.RedeliveryService;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RedeliveryController.class)
class RedeliveryControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  RedeliveryService redeliveryService;

  @Test
  void 登録出来たら201を返す() throws Exception {

    // Given
    RedeliveryResponse created = new RedeliveryResponse("123456789012", LocalDate.of(2026, 10, 29), "午前中");
    when(redeliveryService.create(any(CreateRedeliveryRequest.class))).thenReturn(created);

    // When/Then
    mockMvc.perform(post("/api/redeliveries")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.trackingNumber").value("123456789012"));
  }

  @Test
  void 伝票番号が12桁でなければ400を返す() throws Exception {
    mockMvc.perform(post("/api/redeliveries")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson().replace("\"trackingNumber\": \"123456789012\"", "\"trackingNumber\": \"12345\"")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0]").value("trackingNumber:伝票番号は12桁の数字で入力してください"));

    verify(redeliveryService, never()).create(any());
  }

  @Test
  void ServiceがNotFoundExceptionを投げたら404() throws Exception {

    // Given
    when(redeliveryService.create(any(CreateRedeliveryRequest.class)))
        .thenThrow(new NotFoundException("該当する配送物が見つかりません"));

    // When /Then
    mockMvc.perform(post("/api/redeliveries")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("該当する配送物が見つかりません"));
  }

  @Test
  void ServiceがBadRequestExceptionを投げたら400() throws Exception {

    // Given
    when(redeliveryService.create(any(CreateRedeliveryRequest.class)))
        .thenThrow(new BadRequestException("再配達日は明日以降の日付を選択してください"));

    // When /Then
    mockMvc.perform(post("/api/redeliveries")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("再配達日は明日以降の日付を選択してください"));
  }

  @Test
  void ServiceがConflictExceptionを投げたら409() throws Exception {
    // Given
    when(redeliveryService.create(any(CreateRedeliveryRequest.class)))
        .thenThrow(new ConflictException("不在の配送物のみ再配達を受け付けられます"));

    // When /Then
    mockMvc.perform(post("/api/redeliveries")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("不在の配送物のみ再配達を受け付けられます"));

  }

  private String validJson() {
    return """
        {
          "trackingNumber": "123456789012",
          "callNumber": "09012345678",
          "redeliveryDate": "2026-10-29",
          "redeliveryTime": "午前中"
          }
          """;
  }
}
