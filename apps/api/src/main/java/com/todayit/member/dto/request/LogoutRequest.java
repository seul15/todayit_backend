package com.todayit.member.dto.request;

/**
 * 로그아웃 요청입니다.
 *
 * @param refreshToken 현재 로그인 세션의 Refresh Token
 */
public record LogoutRequest(String refreshToken) {

  /** 로그아웃 요청값을 확인합니다. */
  public void validate() {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("Refresh Token은 필수입니다.");
    }
  }
}
