package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ヘルスチェック", description = "アプリの稼働確認")
@RestController
@RequestMapping("/api")
public class HealthController {

  @Operation(summary = "稼働確認", description = "アプリが動いていればOKを返す")
  @GetMapping("/health")
  public String health() {
    return "OK";
  }
}
