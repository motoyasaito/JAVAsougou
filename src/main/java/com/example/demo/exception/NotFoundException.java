package com.example.demo.exception;

//データが見つからないときに投げる例外。GlobalExeptionHandlerで404に変換する。
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
