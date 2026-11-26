package com.example.demo.service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;

@Service
public class LogUploadService {
  private static final Logger log = LoggerFactory.getLogger(LogUploadService.class);
  private static final Pattern LOG_FILE_NAME = Pattern.compile("^(app|api-access)\\.(\\d{4}-\\d{2}-\\d{2})\\.log$");
  private final S3Client s3Client;
  private final String bucket;
  private final Path logDir;

  public LogUploadService(S3Client s3Client, @Value("${app.log-upload.bucket}") String bucket,
      @Value("${app.log-upload.dir}") Path logDir) {
    this.s3Client = s3Client;
    this.bucket = bucket;
    this.logDir = logDir;
  }

  public void uploadLogs(LocalDate today) throws IOException {
    try (Stream<Path> files = Files.list(logDir)) {
      List<Path> targets = files
          .filter(file -> isUploadTarget(file, today))
          .toList();
      int uploaded = 0;
      int failed = 0;
      for (Path file : targets) {
        try {
          PutObjectRequest request = PutObjectRequest.builder()
              .bucket(bucket)
              .key(toKey(file))
              .build();
          s3Client.putObject(request, RequestBody.fromFile(file));
          Files.delete(file);
          uploaded++;
        } catch (SdkException | IOException e) {
          failed++;
          log.warn("ログファイルをS3に送れませんでした。次回再送信します:{}", file.getFileName(), e);
        }
      }
      if (failed == 0) {
        log.info("全{}件送信しました", uploaded);
      } else {
        log.warn("送信{}件。失敗{}件。失敗したファイルは次回再送信します", uploaded, failed);
      }
    }
  }

  private boolean isUploadTarget(Path file, LocalDate today) {
    String fileName = file.getFileName().toString();
    Matcher m = LOG_FILE_NAME.matcher(fileName);
    if (!m.matches()) {
      return false;
    }
    LocalDate date = LocalDate.parse(m.group(2));
    return date.isBefore(today);
  }

  private String toKey(Path file) {
    String fileName = file.getFileName().toString();
    Matcher m = LOG_FILE_NAME.matcher(fileName);
    m.matches();
    String type = m.group(1);
    LocalDate date = LocalDate.parse(m.group(2));
    return type + "/" + date.getYear() + "/" + String.format("%02d", date.getMonthValue()) + "/" + fileName;
  }
}
