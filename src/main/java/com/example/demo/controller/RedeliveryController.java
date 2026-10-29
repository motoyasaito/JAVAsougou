package com.example.demo.controller;

import com.example.demo.dto.CreateRedeliveryRequest;
import com.example.demo.dto.RedeliveryResponse;
import com.example.demo.service.RedeliveryService;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redeliveries")
public class RedeliveryController {
  private final RedeliveryService redeliveryService;

  public RedeliveryController(RedeliveryService redeliveryService) {
    this.redeliveryService = redeliveryService;
  }

  // 再配達の登録をする。登録出来たら201と登録した内容を返す
  @PostMapping
  public ResponseEntity<RedeliveryResponse> create(@RequestBody @Valid CreateRedeliveryRequest request) {
    RedeliveryResponse created = redeliveryService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
