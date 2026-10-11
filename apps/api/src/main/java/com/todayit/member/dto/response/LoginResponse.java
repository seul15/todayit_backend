package com.todayit.member.dto.response;

import com.todayit.member.service.model.LoginResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 로그인 성공 응답입니다.
 *
 * @param accessToken API 인증에 사용할 Access Token
 * @param refreshToken Access Token 재발급에 사용할 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 * @param member 로그인한 회원 정보
 */
@Schema(description = "로그인 성공 응답 데이터")
public record LoginResponse(
    @Schema(description = "인증이 필요한 API 호출에 사용할 Access Token입니다.", example = "eyJ...")
        String accessToken,
    @Schema(description = "토큰 갱신 및 로그아웃 시 사용할 Refresh Token입니다.", example = "example-refresh-token")
        String refreshToken,
    @Schema(description = "Access Token의 유효 시간(초)입니다. 실제 값은 JWT 설정에 따라 달라집니다.", example = "3600")
        long expiresIn,
    @Schema(description = "로그인한 회원 정보입니다.") MemberResponse member) {

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
  @Schema(description = "로그인한 회원 정보")
  public record MemberResponse(
      @Schema(description = "회원 식별자입니다.", example = "123e4567-e89b-12d3-a456-426614174000")
          String memberId,
      @Schema(description = "회원 닉네임입니다.", example = "오늘이") String nickname,
      @Schema(description = "커플 연결 여부입니다.", example = "false") boolean coupleConnected,
      @Schema(description = "회원에게 부여된 권한 목록입니다.", example = "[\"USER\"]") List<String> roles) {

    private static MemberResponse from(com.todayit.member.service.model.MemberLoginResult member) {
      return new MemberResponse(
          member.memberId(), member.nickname(), member.coupleConnected(), member.roles());
    }
  }
}
