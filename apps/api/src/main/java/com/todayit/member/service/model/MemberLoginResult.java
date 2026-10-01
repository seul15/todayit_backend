package com.todayit.member.service.model;

import java.util.List;

/**
 * 로그인 인증이 성공한 회원의 결과입니다.
 *
 * @param memberId 로그인에 성공한 회원 식별자
 * @param nickname 로그인한 회원 닉네임
 * @param coupleConnected 커플 연결 여부
 * @param roles 로그인에 성공한 회원 권한 목록
 */
public record MemberLoginResult(
    String memberId, String nickname, boolean coupleConnected, List<String> roles) {}
