package com.example.demo.dto;

import java.util.List;

public record ErrorResponse(
    int status,
    String error,
    String message,
    List<String> details) {
}
