package com.todayit.member.service.model;

/**
 * 로그인 인증이 성공한 회원의 결과입니다.
 *
 * @param memberId 로그인에 성공한 회원 식별자
 * @param role 로그인에 성공한 회원 권한
 */
public record MemberLoginResult(String memberId, String role) {}
