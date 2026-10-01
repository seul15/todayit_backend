package com.todayit.member.dto.response;

import com.todayit.member.service.model.LoginResult;
import java.util.List;

/**
 * 로그인 성공 응답입니다.
 *
 * @param accessToken API 인증에 사용할 Access Token
 * @param refreshToken Access Token 재발급에 사용할 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 * @param member 로그인한 회원 정보
 */
public record LoginResponse(
    String accessToken, String refreshToken, long expiresIn, MemberResponse member) {

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
        MemberResponse.from(result.member()));
  }

  /**
   * 로그인한 회원 정보입니다.
   *
   * @param memberId 회원 식별자
   * @param nickname 회원 닉네임
   * @param coupleConnected 커플 연결 여부
   * @param roles 회원 권한 목록
   */
  public record MemberResponse(
      String memberId, String nickname, boolean coupleConnected, List<String> roles) {

    private static MemberResponse from(com.todayit.member.service.model.MemberLoginResult member) {
      return new MemberResponse(
          member.memberId(), member.nickname(), member.coupleConnected(), member.roles());
    }
  }
}
