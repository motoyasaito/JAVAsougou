package com.example.demo.dto;

import com.example.demo.entity.Delivery;
import java.time.LocalDate;

// 配送状況確認で返す1件分の形（ログインなしで呼べるので、個人情報は返さない）
// お客さんの配送状況確認画面でも、この形をそのまま使う
public record DeliveryStatusResponse(
    String trackingNumber,
    LocalDate deliveryDate,
    String deliveryTime,// 配達の時間帯（午前中など）。個人情報ではないので返してよい
    String status) {

  public static DeliveryStatusResponse from(Delivery delivery) {
    return new DeliveryStatusResponse(
        delivery.getTrackingNumber(),
        delivery.getDeliveryDate(),
        delivery.getDeliveryTime(),
        delivery.getStatus());
  }
}
