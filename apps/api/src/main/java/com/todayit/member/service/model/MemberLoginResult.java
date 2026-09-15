package com.todayit.member.service.model;

/**
 * 로그인 인증이 성공한 회원의 결과입니다. 이메일/비밀번호 확인 → 잠금 여부 확인 → 회원 확인 → 권한 확인 → 로그인 가능한 회원
 *
 * @param memberId 로그인에 성공한 회원 식별자
 * @param role 로그인에 성공한 회원 권한
 */
public record MemberLoginResult(String memberId, String role) {}
