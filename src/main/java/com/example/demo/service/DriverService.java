package com.example.demo.service;

import com.example.demo.dto.DriverOptionResponse;
import com.example.demo.mapper.UserMapper;
import java.util.List;
import org.springframework.stereotype.Service;

// ドライバーの情報を扱うService
@Service
public class DriverService {

  private final UserMapper userMapper;

  public DriverService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  // ドライバー全員を、担当エリアの順に返す（割り当てのプルダウンに使う）
  public List<DriverOptionResponse> findDrivers() {
    return userMapper.findDrivers().stream()
        .map(DriverOptionResponse::from)// Entity を画面に渡す形（DTO）に詰め替える
        .toList();
  }
}
