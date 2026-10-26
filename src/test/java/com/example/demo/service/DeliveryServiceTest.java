package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.mapper.DeliveryMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Mapper をモックにして、Service のロジック（日付の補完・offset の計算・ページ数）だけを確認する
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

  @Mock
  DeliveryMapper deliveryMapper;

  @InjectMocks
  DeliveryService deliveryService;

  @Test
  void 日付を指定したらその日付で配達一覧を取得する() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);
    Delivery delivery = new Delivery();
    delivery.setTrackingNumber("123456789012");
    when(deliveryMapper.findByDriverId(1L, date, 10, 0L)).thenReturn(List.of(delivery));
    when(deliveryMapper.countByDriverId(1L, date)).thenReturn(1L);

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(1L, date, 0, 10);

    // Then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).trackingNumber()).isEqualTo("123456789012");
    assertThat(result.totalElements()).isEqualTo(1L);
  }

  @Test
  void 日付を省略したら日本時間の今日の配達一覧を取得する() {
    // Given
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));

    // When
    deliveryService.findByDriver(1L, null, 0, 10);

    // Then
    verify(deliveryMapper).findByDriverId(1L, today, 10, 0L);
    verify(deliveryMapper).countByDriverId(1L, today);
  }

  @Test
  void ページ番号と件数からoffsetとページ数を計算する() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);
    when(deliveryMapper.countByDriverId(1L, date)).thenReturn(25L);

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(1L, date, 2, 10);

    // Then
    verify(deliveryMapper).findByDriverId(1L, date, 10, 20L);
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalPages()).isEqualTo(3);
  }

  @Test
  void ページ番号が大きくてもoffsetが負の数にならない() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);

    // When
    deliveryService.findByDriver(1L, date, Integer.MAX_VALUE, 100);

    // Then
    verify(deliveryMapper).findByDriverId(1L, date, 100, (long) Integer.MAX_VALUE * 100);
  }

  @Test
  void 該当する配達がなければ空の一覧を返す() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(1L, date, 0, 10);

    // Then
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isZero();
    assertThat(result.totalPages()).isZero();
  }
}
