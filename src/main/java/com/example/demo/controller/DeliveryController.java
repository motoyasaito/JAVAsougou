package com.example.demo.controller;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.DeliveryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
  private final DeliveryService deliveryService;

  public DeliveryController(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  @GetMapping
  public PageResponse<DeliveryResponse> findByDriver(
      @RequestParam @Min(1) long driverId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "0") @Min(0) int page) {
    return deliveryService.findByDriver(driverId, date, page, size);
  }
}
