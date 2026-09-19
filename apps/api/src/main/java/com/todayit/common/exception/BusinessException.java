package com.todayit.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 업무 규칙 위반을 나타내는 공통 예외입니다. */
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  /**
   * 오류 코드의 기본 메시지로 업무 예외를 생성합니다.
   *
   * @param errorCode 오류 코드
   */
  public BusinessException(ErrorCode errorCode) {
    this(errorCode, errorCode.message(), Map.of());
  }

  /**
   * 지정한 메시지로 업무 예외를 생성합니다.
   *
   * @param errorCode 오류 코드
   * @param message 외부에 전달할 오류 메시지
   */
  public BusinessException(ErrorCode errorCode, String message) {
    this(errorCode, message, Map.of());
  }

  /**
   * 추가 응답 정보를 포함한 업무 예외를 생성합니다.
   *
   * @param errorCode 오류 코드
   * @param details 오류와 함께 전달할 추가 정보
   */
  public BusinessException(ErrorCode errorCode, Map<String, Object> details) {
    this(errorCode, errorCode.message(), details);
  }

  private BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
    super(message);
    this.errorCode = errorCode;
    this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
  }

  /**
   * 예외의 오류 코드를 반환합니다.
   *
   * @return 오류 코드
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * 오류 응답에 추가할 정보를 반환합니다.
   *
   * @return 추가 오류 정보
   */
  public Map<String, Object> getDetails() {
    return details;
  }
}
