package com.todayit.member.service.command;

/**
 * 로그인 서비스에 전달할 로그인 정보입니다.
 *
 * @param email 로그인 이메일
 * @param password 로그인 비밀번호
 */
public record LoginCommand(String email, String password) {}
