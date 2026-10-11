package com.todayit.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이메일 인증번호 확인 성공 응답입니다.
 *
 * @param verificationToken 이메일 인증 완료 토큰
 * @param expiresIn 인증 완료 토큰 유효 시간(초)
 */
@Schema(description = "이메일 인증번호 확인 성공 응답 데이터")
public record EmailVerificationConfirmResponse(
    @Schema(description = "회원가입 시 제출할 이메일 인증 완료 토큰입니다.", example = "example-verification-token")
        String verificationToken,
    @Schema(description = "인증 완료 토큰의 유효 시간(초)입니다.", example = "1800") long expiresIn) {}
