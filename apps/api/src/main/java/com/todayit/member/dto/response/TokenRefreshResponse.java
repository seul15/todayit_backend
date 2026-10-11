package com.todayit.member.dto.response;

import com.todayit.member.service.model.TokenRefreshResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 토큰 갱신 성공 응답입니다.
 *
 * @param accessToken 새로 발급된 Access Token
 * @param refreshToken 기존 로그인 세션의 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 */
@Schema(description = "토큰 갱신 성공 응답 데이터")
public record TokenRefreshResponse(
    @Schema(description = "새로 발급된 Access Token입니다.", example = "eyJ...") String accessToken,
    @Schema(
            description = "기존 로그인 세션의 Refresh Token입니다. 이 요청에서 변경되지 않습니다.",
            example = "example-refresh-token")
        String refreshToken,
    @Schema(description = "새 Access Token의 유효 시간(초)입니다. 실제 값은 JWT 설정에 따라 달라집니다.", example = "3600")
        long expiresIn) {

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
