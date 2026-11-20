package com.example.demo.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.service.NextDayDeliveryService;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.ZoneId;

@Component
public class NextDayDeliveryBatch {
  private static final Logger log = LoggerFactory.getLogger(NextDayDeliveryBatch.class);
  private final NextDayDeliveryService nextDayDeliveryService;

  public NextDayDeliveryBatch(NextDayDeliveryService nextDayDeliveryService) {
    this.nextDayDeliveryService = nextDayDeliveryService;
  }

  @Scheduled(cron = "${batch.next-day-delivery.cron}")
  public void run() {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    log.info("配送期限切れの配送物の検知を開始します。基準日{}", today);
    try {
      nextDayDeliveryService.detectOverdueDeliveries(today);
      log.info("配送物の検知が完了しました");
    } catch (Exception e) {
      log.error("配送物の検知に失敗しました", e);
    }
  }
}
