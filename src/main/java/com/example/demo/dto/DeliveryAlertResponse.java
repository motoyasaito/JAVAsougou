package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.demo.entity.DeliveryAlert;

// 未完了配送物を一覧で返す1件分の形
public record DeliveryAlertResponse(
    String name,
    String trackingNumber,
    String status,
    LocalDate deliveryDate,
    Long driverId,
    LocalDate detectedDate,
    LocalDateTime updatedAt) {
  public static DeliveryAlertResponse from(DeliveryAlert deliveryAlert) {
    return new DeliveryAlertResponse(
        deliveryAlert.getName(),
        deliveryAlert.getTrackingNumber(),
        deliveryAlert.getStatus(),
        deliveryAlert.getDeliveryDate(),
        deliveryAlert.getDriverId(),
        deliveryAlert.getDetectedDate(),
        deliveryAlert.getUpdatedAt());
  }
}
