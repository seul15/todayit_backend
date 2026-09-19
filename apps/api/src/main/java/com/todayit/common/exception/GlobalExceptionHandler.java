package com.todayit.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 업무 예외를 공통 API 오류 응답으로 변환합니다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 업무 예외를 오류 코드에 맞는 HTTP 응답으로 변환합니다.
   *
   * @param exception 발생한 업무 예외
   * @return 공통 오류 응답
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException exception) {

    Map<String, Object> response = new LinkedHashMap<>();

    response.put("success", false);
    response.put("code", exception.getErrorCode().code());
    response.put("message", exception.getMessage());
    response.putAll(exception.getDetails());

    return ResponseEntity.status(toHttpStatus(exception.getErrorCode().status())).body(response);
  }

  private HttpStatus toHttpStatus(ErrorStatus status) {
    return switch (status) {
      case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
    };
  }
}
