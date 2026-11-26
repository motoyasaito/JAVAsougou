package com.example.demo.batch;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.service.LogUploadService;

@Component
public class LogUploadBatch {
  private static final Logger log = LoggerFactory.getLogger(LogUploadBatch.class);
  private final LogUploadService logUploadService;
  private final Clock clock;

  public LogUploadBatch(LogUploadService logUploadService, Clock clock) {
    this.logUploadService = logUploadService;
    this.clock = clock;
  }

  @Scheduled(cron = "${batch.send-logs.cron}")
  public void run() {
    LocalDate today = LocalDate.now(clock);
    log.info("S3へのログ送信を開始します。基準日{}", today);
    try {
      logUploadService.uploadLogs(today);
      log.info("ログの送信が完了しました。");
    } catch (Exception e) {
      log.error("ログの送信に失敗しました。", e);
    }
  }
}
