package com.todayit.member.service.model;

/**
 * Refresh Token 회전 및 Access Token 갱신 처리 결과입니다.
 *
 * @param accessToken 새로 발급된 Access Token
 * @param refreshToken 새로 회전 발급된 Refresh Token
 * @param expiresIn Access Token의 유효 시간(초)
 */
public record TokenRefreshResult(String accessToken, String refreshToken, long expiresIn) {}
