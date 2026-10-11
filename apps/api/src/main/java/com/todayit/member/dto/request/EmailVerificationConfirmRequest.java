package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.EmailVerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이메일 인증번호 확인 요청입니다.
 *
 * @param email 인증할 이메일
 * @param verificationCode 사용자가 입력한 인증번호
 * @param purpose 이메일 인증 목적
 */
@Schema(description = "이메일 인증번호 확인 요청")
public record EmailVerificationConfirmRequest(
    @Schema(
            description = "인증번호를 발급받은 이메일입니다.",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
    @Schema(
            description = "이메일로 전송된 인증번호입니다. 앞자리 0을 보존하기 위해 문자열로 전달합니다.",
            example = "012345",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String verificationCode,
    @Schema(
            description = "이메일 인증 목적입니다. 현재 SIGNUP만 지원합니다.",
            example = "SIGNUP",
            requiredMode = Schema.RequiredMode.REQUIRED)
        EmailVerificationPurpose purpose) {

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

    // 인증번호는 앞자리 0을 포함할 수 있는 숫자 6자리 문자열
    if (!verificationCode.matches("[0-9]{6}")) {
      throw new InvalidRequestException("인증번호는 숫자 6자리로 입력해 주세요.");
    }

    if (purpose == null) {
      throw new InvalidRequestException("인증 목적은 필수입니다.");
    }
  }
}
