package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.EmailVerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이메일 인증번호 발급 요청입니다.
 *
 * @param email 인증할 이메일
 * @param purpose 이메일 인증 목적
 */
@Schema(description = "이메일 인증번호 발급 요청")
public record EmailVerificationRequest(
    @Schema(
            description = "이메일은 100자 이하로 입력합니다.",
            example = "user@example.com",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
    @Schema(
            description = "이메일 인증 목적입니다. 현재 회원가입(SIGNUP)만 지원합니다.",
            example = "SIGNUP",
            requiredMode = Schema.RequiredMode.REQUIRED)
        EmailVerificationPurpose purpose) {

  /**
   * 이메일 인증번호 발급에 필요한 입력값을 확인합니다.
   *
   * @throws InvalidRequestException 이메일 또는 인증 목적이 없는 경우
   */
  public void validate() {
    if (email == null || email.isBlank()) {
      throw new InvalidRequestException("이메일은 필수입니다.");
    }

    if (email.length() > 100) {
      throw new InvalidRequestException("이메일은 100자 이하로 입력해 주세요.");
    }

    if (purpose == null) {
      throw new InvalidRequestException("인증 목적은 필수입니다.");
    }
  }
}
