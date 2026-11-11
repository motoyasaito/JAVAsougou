package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.springframework.boot.test.system.CapturedOutput;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.example.demo.mapper.DeliveryAlertMapper;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
class NextDayDeliveryServiceTest {

  @Mock
  DeliveryAlertMapper deliveryAlertMapper;

  @InjectMocks
  NextDayDeliveryService nextDayDeliveryService;

  @Test
  void 渡された日付でMapperを呼ぶ() {
    // Given
    LocalDate today = LocalDate.of(2026, 11, 11);
    when(deliveryAlertMapper.insertOverdue(today)).thenReturn(2);

    // When
    nextDayDeliveryService.detectOverdueDeliveries(today);

    // Then
    verify(deliveryAlertMapper).insertOverdue(today);
  }

  @Test
  void 期限切れが1件以上ならWARNでログ表示(CapturedOutput output) {
    // Given
    LocalDate today = LocalDate.of(2026, 11, 11);
    when(deliveryAlertMapper.insertOverdue(today)).thenReturn(2);

    // When
    nextDayDeliveryService.detectOverdueDeliveries(today);

    // Then
    assertThat(output).contains("WARN");
    assertThat(output).contains("2件検知しました");
  }

  @Test
  void 期限切れが0件ならINFOでログ表示(CapturedOutput output) {
    // Given
    LocalDate today = LocalDate.of(2026, 11, 11);
    when(deliveryAlertMapper.insertOverdue(today)).thenReturn(0);

    // When
    nextDayDeliveryService.detectOverdueDeliveries(today);

    // Then
    assertThat(output).contains("INFO");
    assertThat(output).contains("0件検知しました");
  }
}
