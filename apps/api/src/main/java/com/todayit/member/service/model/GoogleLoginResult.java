package com.todayit.member.service.model;

/**
 * Google 로그인 처리 결과입니다.
 *
 * @param isNewMember 신규 회원 여부
 * @param signupToken 신규 회원의 임시 가입 토큰
 * @param loginResult 기존 회원의 로그인 토큰 및 회원 정보
 */
public record GoogleLoginResult(boolean isNewMember, String signupToken, LoginResult loginResult) {
  /**
   * 기존 회원의 Google 로그인 결과를 생성합니다.
   *
   * @param loginResult 발급된 로그인 토큰과 회원 정보
   * @return 기존 회원 로그인 결과
   */
  public static GoogleLoginResult existingMember(LoginResult loginResult) {
    // 기존 회원은 회원가입이 필요 없으므로 signupToken을 반환하지 않음
    return new GoogleLoginResult(false, null, loginResult);
  }

  /**
   * 신규 회원의 Google 인증 결과를 생성합니다.
   *
   * @param signupToken 회원가입용 임시 토큰
   * @return 신규 회원 인증 결과
   */
  public static GoogleLoginResult newMember(String signupToken) {
    // 신규 회원은 아직 DB에 등록되지 않았으므로 로그인 토큰을 발급하지 않음
    return new GoogleLoginResult(true, signupToken, null);
  }
}
