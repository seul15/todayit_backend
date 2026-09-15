package com.todayit.member.dto.response;

/**
 * 로그인 실패 응답입니다.
 *
 * @param success 요청 성공 여부
 * @param code 로그인 실패 코드
 * @param message 로그인 실패 메시지
 * @param failureCount 현재 로그인 실패 횟수
 * @param maxFailureCount 로그인 최대 실패 횟수
 */
public record LoginErrorResponse(
    boolean success, String code, String message, Long failureCount, int maxFailureCount) {

  /**
   * 일반 로그인 실패 응답을 생성합니다.
   *
   * @param message 로그인 실패 메시지
   * @param failureCount 현재 로그인 실패 횟수
   * @return 로그인 실패 응답
   */
  public static LoginErrorResponse failed(String message, Long failureCount) {
    return new LoginErrorResponse(false, "LOGIN_FAILED", message, failureCount, 5);
  }

  /**
   * 로그인 잠금 응답을 생성합니다.
   *
   * @param message 로그인 잠금 메시지
   * @return 로그인 잠금 응답
   */
  public static LoginErrorResponse locked(String message) {
    return new LoginErrorResponse(false, "LOGIN_LOCKED", message, 5L, 5);
  }
}
