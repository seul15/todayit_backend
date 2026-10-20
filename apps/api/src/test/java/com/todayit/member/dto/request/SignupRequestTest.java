package com.todayit.member.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.service.command.SignupCommand;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignupRequestTest {

  private static final String PASSWORD_ERROR = "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.";

  /** 테스트용 LOCAL 회원가입 요청을 생성합니다. */
  private SignupRequest localRequest(String password, String nickname) {
    return new SignupRequest(
        "test@example.com",
        "verification-token",
        password,
        nickname,
        Map.of(
            "TOS", true,
            "ACCOUNT_PRIVACY", true));
  }

  @Test
  @DisplayName("영문·숫자·특수문자가 모두 포함되면 검증에 성공한다")
  void acceptsValidPassword() {
    SignupRequest request = localRequest("Abcd12345!", "오늘이");

    request.validate();

    assertThat(request.nickname()).isEqualTo("오늘이");
  }

  @Test
  @DisplayName("영문이 없으면 회원가입을 거부한다")
  void rejectsPasswordWithoutEnglish() {
    SignupRequest request = localRequest("123456789!", "오늘이");

    assertThatThrownBy(request::validate)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(PASSWORD_ERROR);
  }

  @Test
  @DisplayName("숫자가 없으면 회원가입을 거부한다")
  void rejectsPasswordWithoutNumber() {
    SignupRequest request = localRequest("abcdefghij!", "오늘이");

    assertThatThrownBy(request::validate)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(PASSWORD_ERROR);
  }

  @Test
  @DisplayName("특수문자가 없으면 회원가입을 거부한다")
  void rejectsPasswordWithoutSpecialCharacter() {
    SignupRequest request = localRequest("Abcdef12345", "오늘이");

    assertThatThrownBy(request::validate)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(PASSWORD_ERROR);
  }

  @Test
  @DisplayName("기존 72바이트 제한을 유지한다")
  void rejectsPasswordExceeding72Bytes() {
    SignupRequest request = localRequest("가".repeat(25) + "Ab1!", "오늘이");

    assertThatThrownBy(request::validate)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요.");
  }

  @Test
  @DisplayName("LOCAL 회원가입 시 닉네임 앞뒤 공백을 제거한다")
  void trimsLocalNickname() {
    SignupRequest request = localRequest("Abcd12345!", "  오늘이  ");

    request.validate();

    assertThat(request.nickname()).isEqualTo("오늘이");
    assertThat(request.toCommand().nickname()).isEqualTo("오늘이");
  }

  @Test
  @DisplayName("GOOGLE 회원가입 시 닉네임 앞뒤 공백을 제거한다")
  void trimsGoogleNickname() {
    SignupRequest request =
        new SignupRequest(
            MemberProvider.GOOGLE,
            null,
            null,
            null,
            "google-signup-token",
            "  구글회원  ",
            Map.of(
                "TOS", true,
                "ACCOUNT_PRIVACY", true));

    request.validate();

    assertThat(request.nickname()).isEqualTo("구글회원");
    assertThat(request.toCommand().nickname()).isEqualTo("구글회원");
  }

  @Test
  @DisplayName("닉네임 중간 공백은 허용하지 않는다")
  void rejectsNicknameWithInternalSpaces() {
    SignupRequest request = localRequest("Abcd12345!", "  오 늘이  ");

    assertThatThrownBy(request::validate)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.");
  }

  @Test
  @DisplayName("Command를 직접 생성해도 닉네임 공백을 제거한다")
  void trimsNicknameInCommand() {
    SignupCommand command =
        new SignupCommand(
            "test@example.com", "verification-token", "Abcd12345!", "  오늘이  ", Map.of("TOS", true));

    assertThat(command.nickname()).isEqualTo("오늘이");
  }
}
