package com.todayit.member.dto.response;

import com.todayit.member.service.model.TokenRefreshResult;

/**
 * 토큰 갱신 성공 응답입니다.
 *
 * @param accessToken 새로 발급된 Access Token
 * @param refreshToken 기존 로그인 세션의 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 */
public record TokenRefreshResponse(String accessToken, String refreshToken, long expiresIn) {

  /**
   * 토큰 갱신 처리 결과를 API 응답으로 변환합니다.
   *
   * @param result 토큰 갱신 처리 결과
   * @return 토큰 갱신 성공 응답
   */
  public static TokenRefreshResponse from(TokenRefreshResult result) {
    return new TokenRefreshResponse(
        result.accessToken(), result.refreshToken(), result.expiresIn());
  }
}
