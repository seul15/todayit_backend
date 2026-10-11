package com.todayit.member.dto.response;

import com.todayit.member.entity.EmailVerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이메일 인증번호 발급 응답입니다.
 *
 * @param email 인증할 이메일
 * @param purpose 이메일 인증 목적
 * @param expiresIn 인증번호 유효 시간(초)
 * @param resendAvailableIn 재발급 가능까지 남은 시간(초)
 */
@Schema(description = "이메일 인증번호 발급 성공 응답 데이터")
public record EmailVerificationResponse(
    @Schema(description = "인증번호를 발급한 이메일입니다.", example = "user@example.com") String email,
    @Schema(description = "이메일 인증 목적입니다.", example = "SIGNUP") EmailVerificationPurpose purpose,
    @Schema(description = "인증번호 유효 시간(초)입니다.", example = "1800") long expiresIn,
    @Schema(description = "재발급 대기 시간(초)입니다.", example = "60") long resendAvailableIn) {}
