package com.todayit.member.exception;

import com.todayit.common.exception.BusinessException;

/** 이메일 인증 토큰이 유효하지 않은 경우 발생하는 예외입니다. */
public class InvalidEmailVerificationTokenException extends BusinessException {

  /** 이메일 인증 토큰 오류를 생성합니다. */
  public InvalidEmailVerificationTokenException() {
    super(MemberErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
  }
}
