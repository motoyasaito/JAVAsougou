package com.example.demo.dto;

import java.util.List;

// ページングする一覧で返す形。<T>には一覧の1件分の型（DeliveryResponseなど）が入る
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {

  public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
    // 全件数をページの件数で割って、端数は切り上げる（例: 25件を10件ずつなら3ページ）
    int totalPages = (int) Math.ceil((double) totalElements / size);
    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }
}
