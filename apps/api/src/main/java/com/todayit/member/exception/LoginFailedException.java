package com.todayit.member.exception;

/** 이메일 또는 비밀번호가 올바르지 않은 로그인 요청에서 발생하는 예외입니다. */
public class LoginFailedException extends RuntimeException {

  private final Long failureCount;

  /** 로그인 실패 예외를 생성합니다. */
  public LoginFailedException() {
    super("이메일 또는 비밀번호가 올바르지 않습니다.");
    this.failureCount = null;
  }

  /**
   * 현재 로그인 실패 횟수를 포함한 예외를 생성합니다.
   *
   * @param failureCount 현재 로그인 실패 횟수
   */
  public LoginFailedException(long failureCount) {
    super("이메일 또는 비밀번호가 올바르지 않습니다.");
    this.failureCount = failureCount;
  }

  /**
   * 현재 로그인 실패 횟수를 반환합니다.
   *
   * @return 로그인 실패 횟수, 횟수가 없는 실패라면 null
   */
  public Long getFailureCount() {
    return failureCount;
  }
}
