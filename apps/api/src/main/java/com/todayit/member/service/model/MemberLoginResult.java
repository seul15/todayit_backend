package com.todayit.member.service.model;

/**
 * 로그인 인증이 성공한 회원의 결과입니다.
 *
 * @param memberId 로그인에 성공한 회원 식별자
 */
public record MemberLoginResult(String memberId) {}
