package com.todayit.common.auth.token;

/**
 * Refresh Token 발급 또는 회전 결과입니다.
 *
 * @param token 클라이언트에 전달할 새로운 Refresh Token
 * @param sessionId Refresh Token에 연결된 로그인 세션 ID. 토큰 회전 시에도 기존 세션 ID를 유지합니다.
 */
public record RefreshTokenResult(String token, String sessionId) {}
