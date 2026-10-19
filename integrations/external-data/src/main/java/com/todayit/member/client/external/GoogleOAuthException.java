package com.todayit.member.client.external;

/** Google OAuth 외부 연동 중 발생한 오류입니다. */
public class GoogleOAuthException extends RuntimeException {

  /** Google OAuth 실패 원인입니다. */
  public enum Reason {
    INVALID_AUTHORIZATION_CODE,
    INVALID_ID_TOKEN,
    PROVIDER_UNAVAILABLE
  }

  private final Reason reason;

  /**
   * Google OAuth 오류를 생성합니다.
   *
   * @param reason 실패 원인
   */
  public GoogleOAuthException(Reason reason) {
    super("Google OAuth failed: " + reason);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }
}
