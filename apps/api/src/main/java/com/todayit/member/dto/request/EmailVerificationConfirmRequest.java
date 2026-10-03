package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.EmailVerificationPurpose;

/**
 * 이메일 인증번호 확인 요청입니다.
 *
 * @param email 인증할 이메일
 * @param verificationCode 사용자가 입력한 인증번호
 * @param purpose 이메일 인증 목적
 */
public record EmailVerificationConfirmRequest(
    String email, String verificationCode, EmailVerificationPurpose purpose) {

  /**
   * 이메일 인증번호 확인에 필요한 입력값을 검증합니다.
   *
   * @throws InvalidRequestException 필수 입력값이 없는 경우
   */
  public void validate() {
    if (email == null || email.isBlank()) {
      throw new InvalidRequestException("이메일은 필수입니다.");
    }

    if (verificationCode == null || verificationCode.isBlank()) {
      throw new InvalidRequestException("인증번호는 필수입니다.");
    }

    if (purpose == null) {
      throw new InvalidRequestException("인증 목적은 필수입니다.");
    }
  }
}
