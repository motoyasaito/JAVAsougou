package com.example.demo.dto;

import com.example.demo.entity.DriverOption;

// 画面に渡すドライバー1人分（id・名前・担当エリア）
public record DriverOptionResponse(
    Long id,
    String name,
    String areaName) {

  public static DriverOptionResponse from(DriverOption driver) {
    return new DriverOptionResponse(driver.getId(), driver.getName(), driver.getAreaName());
  }
}
