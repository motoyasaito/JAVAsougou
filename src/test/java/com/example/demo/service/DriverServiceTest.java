package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.demo.dto.DriverOptionResponse;
import com.example.demo.entity.DriverOption;
import com.example.demo.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

  @Mock
  UserMapper userMapper;

  @InjectMocks
  DriverService driverService;

  @Test
  void ドライバー一覧をDTOに変換して返す() {
    DriverOption driver = new DriverOption();
    driver.setId(2L);
    driver.setName("配送 一郎");
    driver.setAreaName("埼玉");
    when(userMapper.findDrivers()).thenReturn(List.of(driver));

    List<DriverOptionResponse> result = driverService.findDrivers();

    assertThat(result).containsExactly(new DriverOptionResponse(2L, "配送 一郎", "埼玉"));
  }

  @Test
  void ドライバーがいなければ空の一覧を返す() {
    when(userMapper.findDrivers()).thenReturn(List.of());

    assertThat(driverService.findDrivers()).isEmpty();
  }
}
