package com.todayit.member.client.external;

/**
 * Google에서 검증한 계정 정보입니다.
 *
 * @param providerUserId Google 계정의 고유 식별자(sub)
 * @param email Google 계정 이메일
 * @param emailVerified Google 이메일 인증 여부
 */
public record GoogleIdentity(String providerUserId, String email, boolean emailVerified) {}
