package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.dto.DeliveryAlertResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.mapper.DeliveryAlertMapper;
import java.util.List;

@Service
public class DeliveryAlertService {

  private final DeliveryAlertMapper deliveryAlertMapper;

  public DeliveryAlertService(DeliveryAlertMapper deliveryAlertMapper) {
    this.deliveryAlertMapper = deliveryAlertMapper;
  }

  public PageResponse<DeliveryAlertResponse> findPending(int page, int size) {
    long offset = (long) page * size;

    List<DeliveryAlertResponse> content = deliveryAlertMapper.findPending(size, offset).stream()
        .map(DeliveryAlertResponse::from)
        .toList();
    long total = deliveryAlertMapper.countPending();

    return PageResponse.of(content, page, size, total);
  }
}
