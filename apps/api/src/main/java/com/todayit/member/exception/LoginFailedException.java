package com.todayit.member.exception;

/** 이메일 또는 비밀번호가 올바르지 않은 로그인 요청에서 발생하는 예외입니다. */
public class LoginFailedException extends RuntimeException {

  /** 로그인 실패 예외를 생성합니다. */
  public LoginFailedException() {
    super("이메일 또는 비밀번호가 올바르지 않습니다.");
  }
}
