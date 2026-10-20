package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.client.external.GoogleIdentity;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.service.command.SignupCommand;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class SignupFacadeTest {

  @Mock private SignupService signupService;
  @Mock private GoogleSignupTokenService googleSignupTokenService;

  private SignupFacade signupFacade;

  private static final Map<String, Boolean> AGREEMENTS =
      Map.of(
          "TOS", true,
          "ACCOUNT_PRIVACY", true);

  @BeforeEach
  void setUp() {
    signupFacade = new SignupFacade(signupService, googleSignupTokenService);
  }

  @Test
  @DisplayName("LOCAL 회원가입은 기존 로컬 가입 서비스를 호출한다")
  void signsUpLocalMember() {
    // Given
    SignupCommand command =
        new SignupCommand(
            "local@test.com", "verification-token", "Password123!", "로컬회원", AGREEMENTS);

    Member member = Member.createLocal("local@test.com", "encoded-password", "로컬회원");

    when(signupService.createLocalMember(command)).thenReturn(member);

    // When
    Member result = signupFacade.signup(command);

    // Then
    assertThat(result).isSameAs(member);

    verify(signupService).createLocalMember(command);

    // LOCAL 가입 시 Google 토큰 서비스는 호출하지 않음
    verifyNoInteractions(googleSignupTokenService);
  }

  @Test
  @DisplayName("GOOGLE 회원가입은 DB 저장 이후 가입 토큰을 무효화한다")
  void signsUpGoogleMember() {
    // Given
    String token = "google-signup-token";

    SignupCommand command =
        new SignupCommand(MemberProvider.GOOGLE, null, null, null, token, "구글회원", AGREEMENTS);

    GoogleIdentity identity = new GoogleIdentity("google-123", "google@test.com", true);

    Member member = Member.createGoogle("google@test.com", "google-123", "구글회원");

    when(googleSignupTokenService.getVerifiedIdentity(token)).thenReturn(identity);

    when(signupService.createGoogleMember(command, identity)).thenReturn(member);

    // When
    Member result = signupFacade.signup(command);

    // Then
    assertThat(result).isSameAs(member);

    // 토큰 검증 → 회원 생성 → 토큰 삭제 순서 확인
    InOrder inOrder = inOrder(googleSignupTokenService, signupService);

    inOrder.verify(googleSignupTokenService).getVerifiedIdentity(token);

    inOrder.verify(signupService).createGoogleMember(command, identity);

    inOrder.verify(googleSignupTokenService).invalidate(token);
  }

  @Test
  @DisplayName("GOOGLE 회원가입 DB 저장 실패 시 가입 토큰을 삭제하지 않는다")
  void preservesTokenWhenGoogleSignupFails() {
    // Given
    String token = "google-signup-token";

    SignupCommand command =
        new SignupCommand(MemberProvider.GOOGLE, null, null, null, token, "구글회원", AGREEMENTS);

    GoogleIdentity identity = new GoogleIdentity("google-123", "google@test.com", true);

    when(googleSignupTokenService.getVerifiedIdentity(token)).thenReturn(identity);

    when(signupService.createGoogleMember(command, identity))
        .thenThrow(new IllegalStateException("DB 저장 실패"));

    // When & Then
    assertThatThrownBy(() -> signupFacade.signup(command))
        .isInstanceOf(IllegalStateException.class);

    // 가입 실패 시 재시도할 수 있도록 토큰 유지
    verify(googleSignupTokenService, never()).invalidate(token);
  }

  @Test
  @DisplayName("가입 성공 후 Redis 정리가 실패해도 회원가입 결과는 유지한다")
  void returnsMemberWhenTokenCleanupFails() {
    // Given
    String token = "google-signup-token";

    SignupCommand command =
        new SignupCommand(MemberProvider.GOOGLE, null, null, null, token, "구글회원", AGREEMENTS);

    GoogleIdentity identity = new GoogleIdentity("google-123", "google@test.com", true);

    Member member = Member.createGoogle("google@test.com", "google-123", "구글회원");

    when(googleSignupTokenService.getVerifiedIdentity(token)).thenReturn(identity);

    when(signupService.createGoogleMember(command, identity)).thenReturn(member);

    doThrow(new DataAccessResourceFailureException("Redis 연결 실패"))
        .when(googleSignupTokenService)
        .invalidate(token);

    // When
    Member result = signupFacade.signup(command);

    // Then
    assertThat(result).isSameAs(member);
  }

  @Test
  @DisplayName("아직 구현되지 않은 KAKAO 회원가입은 거부한다")
  void rejectsUnsupportedProvider() {
    SignupCommand command =
        new SignupCommand(
            MemberProvider.KAKAO, null, null, null, "signup-token", "카카오회원", AGREEMENTS);

    assertThatThrownBy(() -> signupFacade.signup(command))
        .isInstanceOf(InvalidRequestException.class);

    verifyNoInteractions(signupService, googleSignupTokenService);
  }
}
