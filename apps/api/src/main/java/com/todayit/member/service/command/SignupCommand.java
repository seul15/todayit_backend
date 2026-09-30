package com.todayit.member.service.command;

import java.util.Map;

/**
 * 회원가입 서비스에 전달할 회원가입 정보입니다.
 *
 * @param email 회원 이메일
 * @param emailVerificationToken 이메일 인증 완료 토큰
 * @param password 회원 비밀번호
 * @param nickname 회원 닉네임
 * @param agreements 약관 식별자별 동의 여부
 */
public record SignupCommand(
    String email,
    String emailVerificationToken,
    String password,
    String nickname,
    Map<Integer, Boolean> agreements) {}
