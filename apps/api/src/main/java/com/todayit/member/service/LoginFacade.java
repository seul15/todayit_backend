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
   * 이메일과 비밀번호로 회원을 인증하고 로그인 토큰을 발급합니다.
   *
   * @param command 이메일과 비밀번호
   * @return 최종 로그인 결과
   */
  public LoginResult login(LoginCommand command) {
    // 이메일과 비밀번호로 LOCAL 회원 인증
    MemberLoginResult member = memberLoginService.login(command);

    return issueTokens(member);
  }

  /**
   * 인증이 완료된 회원에게 Access Token과 Refresh Token을 발급합니다.
   *
   * <p>로컬 로그인과 소셜 로그인에서 공통으로 사용하며, 호출 전에 회원 인증과 권한 확인이 완료되어야 합니다.
   *
   * @param member 인증이 완료된 회원 정보
   * @return 발급된 토큰과 회원 정보
   */
  public LoginResult issueTokens(MemberLoginResult member) {
    // Refresh Token 및 로그인 세션 생성
    RefreshTokenResult refreshToken = refreshTokenService.create(member.memberId());

    // Access Token 생성
    String accessToken =
        jwtTokenProvider.createAccessToken(
            member.memberId(), member.roles(), refreshToken.sessionId());

    long expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

    return new LoginResult(member, accessToken, refreshToken.token(), expiresIn);
  }
}
