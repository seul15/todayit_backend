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

  /**
   * 실제 발생한 원인 예외를 포함해 Google OAuth 오류를 생성합니다.
   *
   * @param reason 실패 원인
   * @param cause 실제 오류를 발생시킨 원인 예외
   */
  public GoogleOAuthException(Reason reason, Throwable cause) {

    // 원래 오류를 보관해 서버에서 원인을 추적할 수 있도록 함
    super("Google OAuth failed: " + reason, cause);

    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }
}
