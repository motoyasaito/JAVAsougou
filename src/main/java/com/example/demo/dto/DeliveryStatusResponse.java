package com.example.demo.dto;

import com.example.demo.entity.Delivery;
import java.time.LocalDate;

// 配送状況確認で返す1件分の形（ログインなしで呼べるので、個人情報は返さない）
public record DeliveryStatusResponse(
    String trackingNumber,
    LocalDate deliveryDate,
    String status) {

  public static DeliveryStatusResponse from(Delivery delivery) {
    return new DeliveryStatusResponse(
        delivery.getTrackingNumber(),
        delivery.getDeliveryDate(),
        delivery.getStatus());
  }
}
