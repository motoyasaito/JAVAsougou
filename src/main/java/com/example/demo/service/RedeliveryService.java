package com.example.demo.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.dto.RedeliveryResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.entity.Redelivery;
import com.example.demo.dto.CreateRedeliveryRequest;
import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.RedeliveryMapper;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;

@Service
public class RedeliveryService {
  private final DeliveryMapper deliveryMapper;
  private final RedeliveryMapper redeliveryMapper;

  public RedeliveryService(DeliveryMapper deliveryMapper, RedeliveryMapper redeliveryMapper) {
    this.deliveryMapper = deliveryMapper;
    this.redeliveryMapper = redeliveryMapper;
  }

  // 再配達登録し、配送物のステータスをPREPARINGに戻す
  @Transactional
  public RedeliveryResponse create(CreateRedeliveryRequest request) {
    Delivery delivery = deliveryMapper.findByTrackingNumber(request.trackingNumber())
        .orElseThrow(() -> new NotFoundException("該当する配送物が見つかりません"));
    if (!delivery.getCallNumber().equals(request.callNumber())) {
      throw new NotFoundException("該当する配送物が見つかりません");
    }
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    if (!request.redeliveryDate().isAfter(today)) {
      throw new BadRequestException("再配達日は明日以降の日付を選択してください");
    }
    if (!delivery.getStatus().equals("ABSENT")) {
      throw new ConflictException("不在の配送物のみ再配達を受け付けられます");
    }
    Redelivery redelivery = new Redelivery();
    redelivery.setDeliveryId(delivery.getId());
    redelivery.setRedeliveryDate(request.redeliveryDate());
    redelivery.setRedeliveryTime(request.redeliveryTime());
    redeliveryMapper.insert(redelivery);

    deliveryMapper.updateStatus(delivery.getId(), "PREPARING");
    return new RedeliveryResponse(delivery.getTrackingNumber(), request.redeliveryDate(), request.redeliveryTime());
  }
}
