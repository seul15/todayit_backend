package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.EmailVerificationPurpose;

/**
 * 이메일 인증번호 발급 요청입니다.
 *
 * @param email 인증할 이메일
 * @param purpose 이메일 인증 목적
 */
public record EmailVerificationRequest(String email, EmailVerificationPurpose purpose) {

  /**
   * 이메일 인증번호 발급에 필요한 입력값을 확인합니다.
   *
   * @throws InvalidRequestException 이메일 또는 인증 목적이 없는 경우
   */
  public void validate() {
    if (email == null || email.isBlank()) {
      throw new InvalidRequestException("이메일은 필수입니다.");
    }

    if (purpose == null) {
      throw new InvalidRequestException("인증 목적은 필수입니다.");
    }
  }
}
