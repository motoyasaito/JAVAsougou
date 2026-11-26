package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.RedeliveryMapper;
import com.example.demo.mapper.DeliveryAlertMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DeleteCompletedService {
  private static final Logger log = LoggerFactory.getLogger(DeleteCompletedService.class);
  private final RedeliveryMapper redeliveryMapper;
  private final DeliveryMapper deliveryMapper;
  private final DeliveryAlertMapper deliveryAlertMapper;

  public DeleteCompletedService(RedeliveryMapper redeliveryMapper, DeliveryMapper deliveryMapper,
      DeliveryAlertMapper deliveryAlertMapper) {
    this.redeliveryMapper = redeliveryMapper;
    this.deliveryMapper = deliveryMapper;
    this.deliveryAlertMapper = deliveryAlertMapper;
  }

  // 配達完了から基準日時より前に更新された配送物と、そのアラートと再配達を削除する
  @Transactional // 途中で失敗したらすべての処理を取り消す
  public void deleteCompleted(LocalDateTime threshold) {
    int deliveryAlertCount = deliveryAlertMapper.deleteByCompletedDeliveries(threshold);// 先に子（アラート）を消す。戻り値は消した件数。
    int redeliveryCount = redeliveryMapper.deleteByCompletedDeliveries(threshold);// 次に子（再配達）を消す。
    int deliveryCount = deliveryMapper.deleteCompleted(threshold);// 最後に親（配送物）を消す
    log.info("アラートを{}件、再配達を{}件、配送物を{}件削除しました", deliveryAlertCount, redeliveryCount, deliveryCount);// 件数だけログに出す
  }
}
