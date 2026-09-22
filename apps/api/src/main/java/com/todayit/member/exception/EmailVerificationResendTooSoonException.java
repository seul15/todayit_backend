package com.todayit.member.exception;

import com.todayit.common.exception.BusinessException;

/** 이메일 인증번호 재발송 대기 시간이 지나지 않은 경우 발생하는 예외입니다. */
public class EmailVerificationResendTooSoonException extends BusinessException {

  /** 이메일 인증번호 재발송 제한 예외를 생성합니다. */
  public EmailVerificationResendTooSoonException() {
    super(MemberErrorCode.EMAIL_VERIFICATION_RESEND_TOO_SOON);
  }
}
