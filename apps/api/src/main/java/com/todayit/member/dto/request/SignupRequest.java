package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.service.command.SignupCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
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
@Schema(description = "로컬 회원가입 요청")
public record SignupRequest(
    @Schema(
            description = "이메일은 100자 이하로 입력합니다.",
            example = "user@example.com",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
    @Schema(
            description = "이메일 인증번호 확인 후 발급받은 인증 완료 토큰입니다.",
            example = "example-verification-token",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String emailVerificationToken,
    @Schema(
            description = "비밀번호는 10자 이상 72자 이하이며, UTF-8 기준 최대 72바이트입니다.",
            example = "ExamplePassword123!",
            minLength = 10,
            maxLength = 72,
            accessMode = Schema.AccessMode.WRITE_ONLY,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String password,
    @Schema(
            description = "닉네임. 한글, 영문, 숫자만 허용하며 2자 이상 8자 이하입니다. 금칙어와 중복 닉네임은 사용할 수 없습니다.",
            example = "오늘이",
            minLength = 2,
            maxLength = 8,
            pattern = "[가-힣A-Za-z0-9]+",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String nickname,
    @Schema(
            description =
                "약관 코드별 동의 여부입니다. "
                    + "필수: TOS(서비스 이용약관), "
                    + "ACCOUNT_PRIVACY(계정 관련 개인정보 수집·이용 동의)는 true여야 합니다. "
                    + "선택: PREFERENCE(선호정보 수집·이용 동의), "
                    + "PROFILE_IMAGE(프로필 이미지 수집·이용 동의)는 "
                    + "true 또는 false로 전달할 수 있습니다.",
            requiredMode = Schema.RequiredMode.REQUIRED)
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

    if (email.length() > 100) {
      throw new InvalidRequestException("이메일은 100자 이하로 입력해 주세요.");
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

    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new InvalidRequestException("비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요.");
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
