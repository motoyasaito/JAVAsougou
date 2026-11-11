package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.time.LocalDate;

import com.example.demo.mapper.DeliveryAlertMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NextDayDeliveryService {
  private static final Logger log = LoggerFactory.getLogger(NextDayDeliveryService.class);
  private final DeliveryAlertMapper deliveryAlertMapper;

  public NextDayDeliveryService(DeliveryAlertMapper deliveryAlertMapper) {
    this.deliveryAlertMapper = deliveryAlertMapper;
  }

  public void detectOverdueDeliveries(LocalDate today) {
    int overdueCount = deliveryAlertMapper.insertOverdue(today);
    if (overdueCount == 0) {
      log.info("期限切れの配送物を{}件検知しました", overdueCount);
    } else {
      log.warn("期限切れの配送物を{}件検知しました", overdueCount);
    }
  }
}
