package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.service.command.LoginCommand;

/**
 * 로컬 로그인 요청입니다.
 *
 * @param email 로그인 이메일
 * @param password 로그인 비밀번호
 */
public record LoginRequest(String email, String password) {

  /**
   * 요청으로 받은 로그인 정보를 서비스에 전달할 형태로 변환합니다.
   *
   * @return 로그인 서비스에 전달할 정보
   */
  public LoginCommand toCommand() {
    return new LoginCommand(email, password);
  }

  /**
   * 로그인에 필요한 필수 입력을 확인합니다.
   *
   * @throws InvalidRequestException 이메일 또는 비밀번호가 없거나 공백일 때
   */
  public void validate() {
    if (email == null || email.isBlank()) {
      throw new InvalidRequestException("이메일은 필수입니다.");
    }

    if (password == null || password.isBlank()) {
      throw new InvalidRequestException("비밀번호는 필수입니다.");
    }
  }
}
