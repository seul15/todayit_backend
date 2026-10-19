package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 구글 OAuth 로그인 요청입니다.
 *
 * @param authorizationCode Google에서 발급한 인가 코드
 * @param redirectUri 인가 코드를 발급받을 때 사용한 Redirect URI
 */
@Schema(description = "구글 로그인 요청")
public record GoogleLoginRequest(
    @Schema(description = "Google에서 발급한 일회성 인가 코드입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        String authorizationCode,
    @Schema(
            description = "Google Cloud에 등록한 Redirect URI입니다.",
            example = "http://localhost:3000/oauth/google/callback",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String redirectUri) {

  /** 필수 입력값을 검사합니다. */
  public void validate() {
    if (authorizationCode == null || authorizationCode.isBlank()) {
      throw new InvalidRequestException("Google 인가 코드는 필수입니다.");
    }

    if (redirectUri == null || redirectUri.isBlank()) {
      throw new InvalidRequestException("Redirect URI는 필수입니다.");
    }
  }
}
