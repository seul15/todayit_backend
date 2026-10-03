package com.todayit.common.exception;

import com.todayit.common.response.ErrorResponse;
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
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {

    ErrorResponse response =
        ErrorResponse.of(exception.getErrorCode(), exception.getMessage(), exception.getDetails());

    return ResponseEntity.status(toHttpStatus(exception.getErrorCode().status())).body(response);
  }

  private HttpStatus toHttpStatus(ErrorStatus status) {
    return switch (status) {
      case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
