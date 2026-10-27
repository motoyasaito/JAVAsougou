package com.example.demo.dto;

import com.example.demo.entity.Delivery;
import java.time.LocalDate;

// 管理者の配送物一覧で返す1件分の形（createdAt・updatedAtは画面で使わないので返さない）
public record DeliveryResponse(
    Long id,
    String trackingNumber,
    String name,
    String address,
    String callNumber,
    String email,
    String status,
    String itemType,
    Long driverId,
    LocalDate deliveryDate,
    String deliveryTime) {

  public static DeliveryResponse from(Delivery delivery) {
    return new DeliveryResponse(
        delivery.getId(),
        delivery.getTrackingNumber(),
        delivery.getName(),
        delivery.getAddress(),
        delivery.getCallNumber(),
        delivery.getEmail(),
        delivery.getStatus(),
        delivery.getItemType(),
        delivery.getDriverId(),
        delivery.getDeliveryDate(),
        delivery.getDeliveryTime());
  }
}
