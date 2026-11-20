package com.example.demo.dto;

import com.example.demo.entity.Delivery;
import java.time.LocalDateTime;

// 管理者の詳細画面に渡す形。いつもの配送物の項目（DeliveryResponse）に、登録日時と最終更新日時を足したもの
// DeliveryResponse は API と共通なので、そこには足さずに分けている（APIのレスポンスを変えないため）
public record DeliveryDetailResponse(
    DeliveryResponse delivery,// 伝票番号・氏名・状態など、いつもの項目
    LocalDateTime createdAt,// 登録日時
    LocalDateTime updatedAt) {// 最終更新日時（状態の変更やドライバーの割り当てで変わる）

  public static DeliveryDetailResponse from(Delivery delivery) {
    return new DeliveryDetailResponse(DeliveryResponse.from(delivery), delivery.getCreatedAt(), delivery.getUpdatedAt());
  }
}
