package com.todayit.member.dto.response;

import com.todayit.member.entity.EmailVerificationPurpose;

/**
 * 이메일 인증번호 발급 응답입니다.
 *
 * @param email 인증할 이메일
 * @param purpose 이메일 인증 목적
 * @param expiresIn 인증번호 유효 시간(초)
 * @param resendAvailableIn 재발급 가능까지 남은 시간(초)
 */
public record EmailVerificationResponse(
    String email, EmailVerificationPurpose purpose, long expiresIn, long resendAvailableIn) {}
