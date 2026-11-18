package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.DeliveryAlert;
import com.example.demo.mapper.DeliveryAlertMapper;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.DeliveryAlertResponse;

@ExtendWith(MockitoExtension.class)
class DeliveryAlertServiceTest {

  @Mock
  DeliveryAlertMapper deliveryAlertMapper;

  @InjectMocks
  DeliveryAlertService deliveryAlertService;

  @Test
  void 未完了配送物の一覧をDTOに変換して返す() {
    // Given
    DeliveryAlert deliveryAlert = new DeliveryAlert();
    deliveryAlert.setName("テスト太郎");
    deliveryAlert.setTrackingNumber("123456789012");
    deliveryAlert.setDriverId(1L);
    deliveryAlert.setStatus("IN TRANSIT");
    deliveryAlert.setDeliveryDate(LocalDate.of(2026, 11, 15));
    deliveryAlert.setDetectedDate(LocalDate.of(2026, 11, 17));
    deliveryAlert.setUpdatedAt(LocalDateTime.of(2026, 11, 16, 10, 0));
    when(deliveryAlertMapper.findPending(10, 0)).thenReturn(List.of(deliveryAlert));
    when(deliveryAlertMapper.countPending()).thenReturn(1L);

    // When
    PageResponse<DeliveryAlertResponse> result = deliveryAlertService.findPending(0, 10);

    // Then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).name()).isEqualTo("テスト太郎");
    assertThat(result.content().get(0).driverId()).isEqualTo(1L);
    assertThat(result.content().get(0).trackingNumber()).isEqualTo("123456789012");
    assertThat(result.content().get(0).status()).isEqualTo("IN TRANSIT");
    assertThat(result.content().get(0).deliveryDate()).isEqualTo(LocalDate.of(2026, 11, 15));
    assertThat(result.content().get(0).detectedDate()).isEqualTo(LocalDate.of(2026, 11, 17));
    assertThat(result.content().get(0).updatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 16, 10, 0));
    assertThat(result.totalElements()).isEqualTo(1L);
  }

  @Test
  void ページ番号と件数からoffsetとページ数を計算する() {
    // Given
    when(deliveryAlertMapper.countPending()).thenReturn(25L);

    // When
    PageResponse<DeliveryAlertResponse> result = deliveryAlertService.findPending(2, 10);

    // Then
    verify(deliveryAlertMapper).findPending(10, 20L);
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalPages()).isEqualTo(3);
  }

  @Test
  void ページ番号が大きくてもoffsetが負の数にならない() {
    // When
    deliveryAlertService.findPending(Integer.MAX_VALUE, 100);

    // Then
    verify(deliveryAlertMapper).findPending(100, (long) Integer.MAX_VALUE * 100);
  }

  @Test
  void 未完了配送物がなければ空の一覧を返す() {
    // When
    PageResponse<DeliveryAlertResponse> result = deliveryAlertService.findPending(0, 10);

    // Then
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isZero();
    assertThat(result.totalPages()).isZero();
  }
}
