package com.todayit.member.util;

import com.todayit.common.exception.InvalidRequestException;
import java.nio.charset.StandardCharsets;

/** 비밀번호 설정 및 변경 시 적용할 공통 검증 규칙입니다. */
public final class PasswordValidator {

  private PasswordValidator() {}

  /**
   * 비밀번호 길이, 바이트 수 및 필수 문자 조합을 확인합니다.
   *
   * @param password 검증할 비밀번호
   * @throws InvalidRequestException 비밀번호가 규칙을 만족하지 않는 경우
   */
  public static void validate(String password) {

    // 비밀번호 필수 입력 확인
    if (password == null || password.isBlank()) {
      throw new InvalidRequestException("비밀번호는 필수입니다.");
    }

    // 기존 10~72자 제한 유지
    int passwordLength = password.codePointCount(0, password.length());

    if (passwordLength < 10 || passwordLength > 72) {
      throw new InvalidRequestException("비밀번호는 10자 이상 72자 이하로 입력해 주세요.");
    }

    // BCrypt 사용을 위한 기존 UTF-8 72바이트 제한 유지
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new InvalidRequestException("비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요.");
    }

    // 영문 대문자 또는 소문자 최소 1개
    boolean hasEnglish =
        password.chars().anyMatch(ch -> (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'));

    // 숫자 최소 1개
    boolean hasNumber = password.chars().anyMatch(ch -> ch >= '0' && ch <= '9');

    // ASCII 특수문자 최소 1개
    boolean hasSpecial =
        password.chars().anyMatch(ch -> ch >= 33 && ch <= 126 && !Character.isLetterOrDigit(ch));

    // 영문, 숫자, 특수문자 중 하나라도 없으면 거부
    if (!hasEnglish || !hasNumber || !hasSpecial) {
      throw new InvalidRequestException("비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.");
    }
  }
}
