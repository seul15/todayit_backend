package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;

/**
 * 로그아웃 요청입니다.
 *
 * @param refreshToken 현재 로그인 세션의 Refresh Token
 */
public record LogoutRequest(String refreshToken) {

  /**
   * 로그아웃 요청값을 확인합니다.
   *
   * @throws InvalidRequestException Refresh Token이 없거나 공백일 때
   */
  public void validate() {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new InvalidRequestException("Refresh Token은 필수입니다.");
    }
  }
}
