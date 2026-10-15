package com.todayit.member.dto.response;

import com.todayit.member.service.model.TokenRefreshResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Access Token 갱신 및 Refresh Token 회전 성공 응답입니다.
 *
 * <p>새로운 Access Token과 Refresh Token을 함께 반환합니다. 클라이언트는 반환된 토큰을 저장하고, 이후 인증 요청과 토큰 갱신에 사용해야 합니다.
 *
 * @param accessToken 새로 발급된 Access Token
 * @param refreshToken 새로 발급된 Refresh Token
 * @param expiresIn Access Token 유효 시간(초)
 */
@Schema(description = "Access Token 갱신 및 Refresh Token 회전 성공 응답")
public record TokenRefreshResponse(
    @Schema(description = "새로 발급된 Access Token입니다.", example = "eyJ...") String accessToken,
    @Schema(
            description = "새로 발급된 Refresh Token입니다. 기존 Refresh Token은 즉시 무효화됩니다.",
            example = "new-refresh-token")
        String refreshToken,
    @Schema(description = "새 Access Token의 유효 시간(초)입니다. 실제 값은 JWT 설정에 따라 달라집니다.", example = "3600")
        long expiresIn) {

  /**
   * 토큰 갱신 처리 결과를 API 응답 DTO로 변환합니다.
   *
   * @param result 새 Access Token과 Refresh Token을 포함한 갱신 결과
   * @return 클라이언트에 전달할 토큰 갱신 성공 응답
   */
  public static TokenRefreshResponse from(TokenRefreshResult result) {
    return new TokenRefreshResponse(
        result.accessToken(), result.refreshToken(), result.expiresIn());
  }
}
