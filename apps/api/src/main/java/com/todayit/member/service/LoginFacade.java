package com.todayit.member.service;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenResult;
import com.todayit.common.auth.token.RefreshTokenService;
import com.todayit.member.service.command.LoginCommand;
import com.todayit.member.service.model.LoginResult;
import com.todayit.member.service.model.MemberLoginResult;
import org.springframework.stereotype.Service;

/** 회원 인증과 Access Token 발급을 연결합니다. */
@Service
public class LoginFacade {

  private final MemberLoginService memberLoginService;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;

  /**
   * 로그인 인증 서비스와 토큰 발급 서비스를 받습니다.
   *
   * @param memberLoginService 회원 로그인 인증 Service
   * @param jwtTokenProvider Access Token 발급기
   * @param refreshTokenService Refresh Token 관리 Service
   */
  public LoginFacade(
      MemberLoginService memberLoginService,
      JwtTokenProvider jwtTokenProvider,
      RefreshTokenService refreshTokenService) {
    this.memberLoginService = memberLoginService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.refreshTokenService = refreshTokenService;
  }

  /**
   * 회원을 인증하고 로그인에 필요한 토큰을 발급합니다.
   *
   * @param command 이메일과 비밀번호
   * @return 최종 로그인 결과
   */
  public LoginResult login(LoginCommand command) {

    // 로그인 가능한 회원인지 인증
    MemberLoginResult member = memberLoginService.login(command);

    // Refresh Token과 로그인 세션 생성
    RefreshTokenResult refreshToken = refreshTokenService.create(member.memberId());

    // 로그인 세션 ID를 포함해 Access Token 생성
    String accessToken =
        jwtTokenProvider.createAccessToken(
            member.memberId(), member.roles(), refreshToken.sessionId());

    // Access Token 사용 가능 시간 확인
    long expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

    // 회원 정보와 두 토큰을 최종 로그인 결과로 반환
    return new LoginResult(
        member.memberId(), member.roles(), accessToken, refreshToken.token(), expiresIn);
  }
}
