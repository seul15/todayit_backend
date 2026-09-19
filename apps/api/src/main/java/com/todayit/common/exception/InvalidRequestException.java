package com.todayit.common.exception;

/** API 필수 입력값이 올바르지 않을 때 발생하는 예외입니다. */
public class InvalidRequestException extends BusinessException {

  /**
   * 잘못된 요청 예외를 생성합니다.
   *
   * @param message 잘못된 입력에 대한 메시지
   */
  public InvalidRequestException(String message) {
    super(CommonErrorCode.INVALID_REQUEST, message);
  }
}
