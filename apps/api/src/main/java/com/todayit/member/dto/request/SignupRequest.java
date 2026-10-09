package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.service.command.SignupCommand;
import java.util.Map;

/**
 * 로컬 회원가입 요청입니다.
 *
 * @param email 회원 이메일
 * @param emailVerificationToken 이메일 인증 완료 토큰
 * @param password 회원 비밀번호
 * @param nickname 회원 닉네임
 * @param agreements 약관 식별자별 동의 여부
 */
public record SignupRequest(
    String email,
    String emailVerificationToken,
    String password,
    String nickname,
    Map<String, Boolean> agreements) {

  /**
   * 회원가입 요청을 서비스에 전달할 형태로 변환합니다.
   *
   * @return 회원가입 서비스에 전달할 정보
   */
  public SignupCommand toCommand() {
    return new SignupCommand(email, emailVerificationToken, password, nickname, agreements);
  }

  /**
   * 회원가입에 필요한 필수 입력값을 확인합니다.
   *
   * @throws InvalidRequestException 필수 입력값이 없거나 올바르지 않은 경우
   */
  public void validate() {
    if (email == null || email.isBlank()) {
      throw new InvalidRequestException("이메일은 필수입니다.");
    }

    if (emailVerificationToken == null || emailVerificationToken.isBlank()) {
      throw new InvalidRequestException("이메일 인증 토큰은 필수입니다.");
    }

    if (password == null || password.isBlank()) {
      throw new InvalidRequestException("비밀번호는 필수입니다.");
    }

    int passwordLength = password.codePointCount(0, password.length());
    if (passwordLength < 10 || passwordLength > 72) {
      throw new InvalidRequestException("비밀번호는 10자 이상 72자 이하로 입력해 주세요.");
    }

    if (nickname == null || nickname.isBlank()) {
      throw new InvalidRequestException("닉네임은 필수입니다.");
    }

    if (nickname.length() < 2 || nickname.length() > 8) {
      throw new InvalidRequestException("닉네임은 2자 이상 8자 이하이어야 합니다.");
    }

    if (!nickname.matches("[가-힣A-Za-z0-9]+")) {
      throw new InvalidRequestException("닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.");
    }

    if (agreements == null || agreements.isEmpty()) {
      throw new InvalidRequestException("약관 동의 정보는 필수입니다.");
    }
  }
}
