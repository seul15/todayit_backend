package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그아웃 요청입니다.
 *
 * @param refreshToken 현재 로그인 세션의 Refresh Token
 */
@Schema(description = "로그아웃 요청")
public record LogoutRequest(
    @Schema(
            description =
                "현재 기기의 로그인 세션을 종료할 Refresh Token입니다. Access Token은 Authorization 헤더로 전달합니다.",
            example = "example-refresh-token",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken) {

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
