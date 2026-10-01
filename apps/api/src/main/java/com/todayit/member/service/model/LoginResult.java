package com.todayit.member.service.model;

/**
 * 최종 로그인 처리 결과입니다.
 *
 * @param member 로그인한 회원 정보
 * @param accessToken 발급된 Access Token
 * @param refreshToken 발급된 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 */
public record LoginResult(
    MemberLoginResult member, String accessToken, String refreshToken, long expiresIn) {}
