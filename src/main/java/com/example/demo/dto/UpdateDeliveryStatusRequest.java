package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateDeliveryStatusRequest(
    @NotBlank(message = "ステータスを入力してください") @Pattern(regexp = "PREPARING|IN TRANSIT|DELIVERED|ABSENT", message = "ステータスの指定が正しくありません") String status) {
}
