package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 토큰 갱신 요청입니다.
 *
 * @param refreshToken 로그인 세션의 Refresh Token
 */
@Schema(description = "Access Token 갱신 요청")
public record TokenRefreshRequest(
    @Schema(
            description = "로그인 시 발급받은 Refresh Token입니다. Access Token을 별도로 요구하지 않습니다.",
            example = "example-refresh-token",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken) {

  /**
   * 토큰 갱신 요청값을 확인합니다.
   *
   * @throws InvalidRequestException Refresh Token이 없거나 공백일 때
   */
  public void validate() {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new InvalidRequestException("Refresh Token은 필수입니다.");
    }
  }
}
