package com.example.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Redelivery {
  private Long id;
  private Long deliveryId;
  private String allocationStatus;
  private LocalDate redeliveryDate;
  private String redeliveryTime;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getDeliveryId() {
    return deliveryId;
  }

  public void setDeliveryId(Long deliveryId) {
    this.deliveryId = deliveryId;
  }

  public String getAllocationStatus() {
    return allocationStatus;
  }

  public void setAllocationStatus(String allocationStatus) {
    this.allocationStatus = allocationStatus;
  }

  public LocalDate getRedeliveryDate() {
    return redeliveryDate;
  }

  public void setRedeliveryDate(LocalDate rederiveryDate) {
    this.redeliveryDate = rederiveryDate;
  }

  public String getRedeliveryTime() {
    return redeliveryTime;
  }

  public void setRedeliveryTime(String rederiveryTime) {
    this.redeliveryTime = rederiveryTime;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedTime) {
    this.updatedAt = updatedTime;
  }
}
