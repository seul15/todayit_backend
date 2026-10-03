package com.todayit.member.dto.response;

/**
 * 이메일 인증번호 확인 성공 응답입니다.
 *
 * @param verificationToken 이메일 인증 완료 토큰
 * @param expiresIn 인증 완료 토큰 유효 시간(초)
 */
public record EmailVerificationConfirmResponse(String verificationToken, long expiresIn) {}
