package com.todayit.member.exception;

import com.todayit.common.exception.ErrorCode;
import com.todayit.common.exception.ErrorStatus;

/** 회원 기능에서 사용하는 오류 코드입니다. */
public enum MemberErrorCode implements ErrorCode {
  LOGIN_FAILED("LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다.", ErrorStatus.UNAUTHORIZED),

  LOGIN_LOCKED("LOGIN_LOCKED", "로그인 시도 횟수를 초과했습니다. 비밀번호를 변경해주세요.", ErrorStatus.UNAUTHORIZED);

  private final String code;
  private final String message;
  private final ErrorStatus status;

  MemberErrorCode(String code, String message, ErrorStatus status) {
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
