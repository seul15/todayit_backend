package com.todayit.member.exception;

import com.todayit.common.exception.BusinessException;
import java.util.Map;

/** 입력한 이메일 인증번호가 올바르지 않은 경우 발생하는 예외입니다. */
public class InvalidEmailVerificationCodeException extends BusinessException {

  /**
   * 인증번호 불일치 예외를 생성합니다.
   *
   * @param failureCount 현재 실패 횟수
   */
  public InvalidEmailVerificationCodeException(long failureCount) {
    super(
        MemberErrorCode.EMAIL_VERIFICATION_CODE_INVALID,
        Map.of("failureCount", failureCount, "maxFailureCount", 3));
  }
}
