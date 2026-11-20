package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.RedeliveryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DeleteCompletedService {
  private static final Logger log = LoggerFactory.getLogger(DeleteCompletedService.class);
  private final RedeliveryMapper redeliveryMapper;
  private final DeliveryMapper deliveryMapper;

  public DeleteCompletedService(RedeliveryMapper redeliveryMapper, DeliveryMapper deliveryMapper) {
    this.redeliveryMapper = redeliveryMapper;
    this.deliveryMapper = deliveryMapper;
  }

  // 配達完了から基準日時より前に更新された配送物と、その再配達を削除する
  @Transactional // 途中で失敗したら両方の処理を取り消す
  public void deleteCompleted(LocalDateTime threshold) {
    int redeliveryCount = redeliveryMapper.deleteByCompletedDeliveries(threshold);// 先に子（再配達）を消す。戻り値は消した件数。
    int deliveryCount = deliveryMapper.deleteCompleted(threshold);// 次に親（配送物）を消す
    log.info("再配達を{}件、配送物を{}件削除しました", redeliveryCount, deliveryCount);// 件数だけログに出す
  }
}
