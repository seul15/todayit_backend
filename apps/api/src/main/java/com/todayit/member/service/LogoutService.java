package com.todayit.member.service;

import com.todayit.common.auth.token.RefreshTokenService;
import org.springframework.stereotype.Service;

/** 현재 로그인 세션의 로그아웃을 처리합니다. */
@Service
public class LogoutService {

  private final RefreshTokenService refreshTokenService;

  /**
   * Refresh Token 관리 서비스를 받습니다.
   *
   * @param refreshTokenService Refresh Token 관리 Service
   */
  public LogoutService(RefreshTokenService refreshTokenService) {
    this.refreshTokenService = refreshTokenService;
  }

  /**
   * 현재 기기의 로그인 세션을 종료합니다.
   *
   * @param memberId 로그인한 회원 식별자
   * @param refreshToken 현재 기기의 Refresh Token
   */
  public void logout(String memberId, String refreshToken) {

    // 현재 회원이 사용 중인 Refresh Token 하나만 삭제
    refreshTokenService.delete(refreshToken, memberId);
  }
}
