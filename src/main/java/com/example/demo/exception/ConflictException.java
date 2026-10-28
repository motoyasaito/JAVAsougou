package com.example.demo.exception;

// データを受け付けられないときに投げる例外クラス。GlobalExceptionHandlerで409に変換する
public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
