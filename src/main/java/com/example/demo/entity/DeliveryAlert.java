package com.example.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DeliveryAlert {
  private String name;
  private String trackingNumber;
  private String status;
  private LocalDate deliveryDate;
  private Long driverId;
  private LocalDate detectedDate;
  private LocalDateTime updatedAt;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public void setTrackingNumber(String trackingNumber) {
    this.trackingNumber = trackingNumber;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getDeliveryDate() {
    return deliveryDate;
  }

  public void setDeliveryDate(LocalDate deliveryDate) {
    this.deliveryDate = deliveryDate;
  }

  public Long getDriverId() {
    return driverId;
  }

  public void setDriverId(Long driverId) {
    this.driverId = driverId;
  }

  public LocalDate getDetectedDate() {
    return detectedDate;
  }

  public void setDetectedDate(LocalDate detectedDate) {
    this.detectedDate = detectedDate;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

}
