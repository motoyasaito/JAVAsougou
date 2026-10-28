package com.example.demo.dto;

import java.time.LocalDate;

public record RedeliveryResponse(
    String trackingNumber,
    LocalDate redeliveryDate,
    String redeliveryTime) {
}
