package com.todayit.common.exception;

/** 여러 기능에서 공통으로 사용하는 오류 코드입니다. */
public enum CommonErrorCode implements ErrorCode {
  INVALID_REQUEST("INVALID_REQUEST", "요청값이 올바르지 않습니다.", ErrorStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final ErrorStatus status;

  CommonErrorCode(String code, String message, ErrorStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public ErrorStatus status() {
    return status;
  }
}
