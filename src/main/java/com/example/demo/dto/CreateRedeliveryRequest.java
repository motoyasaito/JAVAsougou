package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record CreateRedeliveryRequest(
    @NotBlank(message = "伝票番号を入力してください") @Pattern(regexp = "^[0-9]{12}$", message = "伝票番号は12桁の数字で入力してください") String trackingNumber,
    @NotBlank(message = "電話番号を入力してください") @Pattern(regexp = "^[0-9]{10,11}$", message = "電話番号はハイフンなしの10桁または11桁の数字で入力してください") String callNumber,
    @NotNull(message = "再配達日を選択してください") LocalDate redeliveryDate,
    @NotBlank(message = "時間帯を入力してください") @Pattern(regexp = "午前中|12:00-14:00|14:00-16:00|16:00-18:00|18:00-20:00", message = "時間帯の指定が正しくありません") String redeliveryTime) {
}
