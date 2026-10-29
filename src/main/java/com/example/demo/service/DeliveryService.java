package com.example.demo.service;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.mapper.DeliveryMapper;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {

  private final DeliveryMapper deliveryMapper;

  public DeliveryService(DeliveryMapper deliveryMapper) {
    this.deliveryMapper = deliveryMapper;
  }

  // 配送物の一覧を、page（0から始まるページ番号）とsize（1ページの件数）で取得する
  public PageResponse<DeliveryResponse> findAll(int page, int size) {
    long offset = (long) page * size; // 例: page=2, size=10 なら21件目から（20件飛ばす）

    List<DeliveryResponse> content = deliveryMapper.findAll(size, offset).stream()
        .map(DeliveryResponse::from)
        .toList();
    long total = deliveryMapper.count();

    return PageResponse.of(content, page, size, total);
  }

  public PageResponse<DeliveryResponse> findByDriver(long driverId, LocalDate deliveryDate, int page, int size) {
    LocalDate targetDate;
    if (deliveryDate == null) {
      targetDate = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    } else {
      targetDate = deliveryDate;
    }
    long offset = (long) page * size;

    List<DeliveryResponse> content = deliveryMapper.findByDriverId(driverId, targetDate, size, offset).stream()
        .map(DeliveryResponse::from)
        .toList();
    long total = deliveryMapper.countByDriverId(driverId, targetDate);

    return PageResponse.of(content, page, size, total);
  }
}
