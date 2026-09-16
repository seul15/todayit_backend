package com.todayit.common.auth.token;

/**
 * Refresh Token 발급 결과입니다.
 *
 * @param token 클라이언트에 전달할 Refresh Token
 * @param sessionId 로그인 세션 식별자
 */
public record RefreshTokenResult(String token, String sessionId) {}
