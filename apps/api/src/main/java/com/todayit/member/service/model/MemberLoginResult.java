package com.todayit.member.service.model;

import java.util.List;

/**
 * 로그인 인증이 성공한 회원의 결과입니다.
 *
 * @param memberId 로그인에 성공한 회원 식별자
 * @param roles 로그인에 성공한 회원 권한 목록
 */
public record MemberLoginResult(String memberId, List<String> roles) {}
