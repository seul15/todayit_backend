package com.todayit.member.exception;

/** 로그인 실패 횟수 초과로 계정이 잠긴 경우 발생하는 예외입니다. */
public class LoginLockedException extends RuntimeException {

  /** 로그인 잠금 예외를 생성합니다. */
  public LoginLockedException() {
    super("로그인 시도 횟수를 초과했습니다. 비밀번호를 변경해주세요.");
  }
}
