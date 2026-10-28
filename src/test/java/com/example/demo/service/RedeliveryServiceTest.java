package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import com.example.demo.dto.CreateRedeliveryRequest;
import com.example.demo.dto.RedeliveryResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.entity.Redelivery;
import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.RedeliveryMapper;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedeliveryServiceTest {

  @Mock
  RedeliveryMapper redeliveryMapper;

  @Mock
  DeliveryMapper deliveryMapper;

  @InjectMocks
  RedeliveryService redeliveryService;

  @Test
  void 登録_条件を満たせば再配達を登録してstatusをPREPARINGに戻す() {
    // Given
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(absentDelivery()));

    // When
    CreateRedeliveryRequest request = createRedeliveryRequest();
    RedeliveryResponse result = redeliveryService.create(request);

    // Then
    ArgumentCaptor<Redelivery> captor = ArgumentCaptor.forClass(Redelivery.class);
    verify(redeliveryMapper).insert(captor.capture());
    assertThat(captor.getValue().getDeliveryId()).isEqualTo(1L);
    assertThat(captor.getValue().getRedeliveryDate()).isEqualTo(request.redeliveryDate());
    assertThat(captor.getValue().getRedeliveryTime()).isEqualTo(request.redeliveryTime());
    verify(deliveryMapper).updateStatus(1L, "PREPARING");
    assertThat(result.trackingNumber()).isEqualTo("123456789012");
  }

  @Test
  void 登録_伝票番号が見つからなければ404の例外を投げる() {
    // Given
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> redeliveryService.create(createRedeliveryRequest()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void 登録_電話番号が違う場合404の例外を投げる() {
    // Given
    Delivery delivery = absentDelivery();
    delivery.setCallNumber("08011112222");
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(delivery));

    // When / Then
    assertThatThrownBy(() -> redeliveryService.create(createRedeliveryRequest()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void 登録_ステータスが不在でない場合409の例外を投げる() {
    // Given
    Delivery delivery = absentDelivery();
    delivery.setStatus("DELIVERED");
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(delivery));

    // When/Then
    assertThatThrownBy(() -> redeliveryService.create(createRedeliveryRequest()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void 登録_再配達が今日の場合400を投げる() {

    // Given
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(absentDelivery()));
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    CreateRedeliveryRequest request = new CreateRedeliveryRequest("123456789012", "08055551122", today, "午前中");

    // When/Then
    assertThatThrownBy(() -> redeliveryService.create(request))
        .isInstanceOf(BadRequestException.class);
  }

  private CreateRedeliveryRequest createRedeliveryRequest() {
    return new CreateRedeliveryRequest("123456789012", "08055551122",
        LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(1), "午前中");
  }

  private Delivery absentDelivery() {
    Delivery delivery = new Delivery();
    delivery.setId(1L);
    delivery.setTrackingNumber("123456789012");
    delivery.setCallNumber("08055551122");
    delivery.setStatus("ABSENT");
    return delivery;
  }
}
