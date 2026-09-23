package com.todayit.member.exception;

import com.todayit.common.exception.ErrorCode;
import com.todayit.common.exception.ErrorStatus;

/** 회원 기능에서 사용하는 오류 코드입니다. */
public enum MemberErrorCode implements ErrorCode {
  LOGIN_FAILED("LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다.", ErrorStatus.UNAUTHORIZED),

  LOGIN_LOCKED("LOGIN_LOCKED", "로그인 시도 횟수를 초과했습니다. 비밀번호를 변경해주세요.", ErrorStatus.UNAUTHORIZED),

  EMAIL_VERIFICATION_CODE_EXPIRED(
      "EMAIL_VERIFICATION_CODE_EXPIRED", "인증번호가 만료되었거나 존재하지 않습니다.", ErrorStatus.BAD_REQUEST),

  EMAIL_VERIFICATION_CODE_INVALID(
      "EMAIL_VERIFICATION_CODE_INVALID", "인증번호가 올바르지 않습니다.", ErrorStatus.BAD_REQUEST),

  EMAIL_VERIFICATION_RESEND_TOO_SOON(
      "EMAIL_VERIFICATION_RESEND_TOO_SOON", "인증번호는 1분 후 다시 요청할 수 있습니다.", ErrorStatus.BAD_REQUEST),

  EMAIL_VERIFICATION_TOKEN_INVALID(
      "EMAIL_VERIFICATION_TOKEN_INVALID", "이메일 인증 정보가 유효하지 않습니다.", ErrorStatus.BAD_REQUEST);

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
