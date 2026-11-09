package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public record AssignDriverRequest(
    @NotNull(message = "ドライバーを選択してください") Long driverId) {
}
