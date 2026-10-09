package com.todayit.member.service.model;

/**
 * 토큰 갱신 처리 결과입니다.
 *
 * @param accessToken 새로 발급된 Access Token
 * @param refreshToken 기존 로그인 세션의 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 */
public record TokenRefreshResult(String accessToken, String refreshToken, long expiresIn) {}
