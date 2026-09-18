package com.todayit.member.dto.response;

import com.todayit.member.service.model.LoginResult;
import java.util.List;

/**
 * 로그인 성공 응답입니다.
 *
 * @param accessToken API 인증에 사용할 Access Token
 * @param refreshToken Access Token 재발급에 사용할 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 * @param memberId 로그인한 회원 식별자
 * @param roles 로그인한 회원 권한 목록
 */
public record LoginResponse(
    String accessToken, String refreshToken, long expiresIn, String memberId, List<String> roles) {
  /**
   * 로그인 처리 결과를 API 응답으로 변환합니다.
   *
   * @param result 로그인 처리 결과
   * @return 로그인 성공 응답
   */
  public static LoginResponse from(LoginResult result) {
    return new LoginResponse(
        result.accessToken(),
        result.refreshToken(),
        result.expiresIn(),
        result.memberId(),
        result.roles());
  }
}
