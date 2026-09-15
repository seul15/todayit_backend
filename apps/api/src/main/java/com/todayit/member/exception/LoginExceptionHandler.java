package com.todayit.member.exception;

import com.todayit.member.dto.response.LoginErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 로그인 처리 중 발생한 예외를 API 오류 응답으로 변환합니다. */
@RestControllerAdvice
public class LoginExceptionHandler {

  /**
   * 이메일 또는 비밀번호가 올바르지 않은 경우 로그인 실패 응답을 반환합니다.
   *
   * @param exception 로그인 실패 예외
   * @return 로그인 실패 응답
   */
  @ExceptionHandler(LoginFailedException.class)
  public ResponseEntity<LoginErrorResponse> handleLoginFailed(LoginFailedException exception) {

    // 로그인 실패 -> 현재 실패 횟수를 프론트에 전달
    LoginErrorResponse response =
        LoginErrorResponse.failed(exception.getMessage(), exception.getFailureCount());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  /**
   * 로그인 실패 횟수가 최대 횟수에 도달한 경우 잠금 응답을 반환합니다.
   *
   * @param exception 로그인 잠금 예외
   * @return 로그인 잠금 응답
   */
  @ExceptionHandler(LoginLockedException.class)
  public ResponseEntity<LoginErrorResponse> handleLoginLocked(LoginLockedException exception) {

    // 로그인 5회 실패 -> 계정 차단 -> 비밀번호 변경 필요
    LoginErrorResponse response = LoginErrorResponse.locked(exception.getMessage());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }
}
