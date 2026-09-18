package com.todayit.member.service.model;

import java.util.List;

/**
 * 최종 로그인 처리 결과입니다.
 *
 * @param memberId 로그인한 회원 식별자
 * @param roles 로그인한 회원 권한 목록
 * @param accessToken 발급된 Access Token
 * @param refreshToken 발급된 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 */
public record LoginResult(
    String memberId, List<String> roles, String accessToken, String refreshToken, long expiresIn) {}
