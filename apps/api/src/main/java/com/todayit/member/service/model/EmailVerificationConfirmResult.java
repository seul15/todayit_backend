package com.todayit.member.service.model;

/**
 * 이메일 인증번호 확인 결과입니다.
 *
 * @param verified 인증 성공 여부
 * @param verificationToken 인증 성공 시 발급된 토큰
 */
public record EmailVerificationConfirmResult(boolean verified, String verificationToken) {
  /**
   * 이메일 인증 성공 결과를 생성합니다.
   *
   * @param verificationToken 인증 완료 토큰
   * @return 인증 성공 결과
   */
  public static EmailVerificationConfirmResult verified(String verificationToken) {
    return new EmailVerificationConfirmResult(true, verificationToken);
  }

  /**
   * 인증번호 재발급 결과를 생성합니다.
   *
   * @return 인증번호 재발급 결과
   */
  public static EmailVerificationConfirmResult reissued() {
    return new EmailVerificationConfirmResult(false, null);
  }
}
