package com.todayit.member.exception;

import com.todayit.common.exception.BusinessException;

/** 이메일 인증번호가 만료되었거나 존재하지 않는 경우 발생하는 예외입니다. */
public class EmailVerificationCodeExpiredException extends BusinessException {

  /** 이메일 인증번호 만료 예외를 생성합니다. */
  public EmailVerificationCodeExpiredException() {
    super(MemberErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
  }
}
