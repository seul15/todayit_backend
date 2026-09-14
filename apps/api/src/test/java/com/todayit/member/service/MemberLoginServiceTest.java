package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.auth.service.LoginAttemptService;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.exception.LoginFailedException;
import com.todayit.member.exception.LoginLockedException;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.command.LoginCommand;
import com.todayit.member.service.model.MemberLoginResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberLoginServiceTest {

  @Mock private MemberRepository memberRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private LoginAttemptService loginAttemptService;

  @Mock private Member member;

  private MemberLoginService memberLoginService;

  @BeforeEach
  void setUp() {
    memberLoginService =
        new MemberLoginService(memberRepository, passwordEncoder, loginAttemptService);
  }

  @Test
  @DisplayName("LOCAL 활성 회원의 비밀번호가 일치하면 로그인에 성공한다")
  void logsInWhenLocalActiveMemberPasswordMatches() {
    // Given
    String email = "test@test.com";
    String password = "password1234";
    String encodedPassword = "encoded-password";
    String memberId = "member-1";
    LoginCommand command = new LoginCommand(email, password);

    when(loginAttemptService.isLocked(email)).thenReturn(false);
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(member.getProvider()).thenReturn(MemberProvider.LOCAL);
    when(member.isActive()).thenReturn(true);
    when(member.getPassword()).thenReturn(encodedPassword);
    when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
    when(member.getId()).thenReturn(memberId);

    // When
    MemberLoginResult result = memberLoginService.login(command);

    // Then
    assertEquals(memberId, result.memberId());
    verify(loginAttemptService).resetFailures(email);
    verify(loginAttemptService, never()).recordFailure(email);
  }

  @Test
  @DisplayName("로그인 실패 횟수 초과로 잠긴 계정은 로그인을 차단한다")
  void throwsLoginLockedExceptionWhenAccountIsLocked() {
    // Given
    String email = "test@test.com";
    String password = "password1234";
    LoginCommand command = new LoginCommand(email, password);

    when(loginAttemptService.isLocked(email)).thenReturn(true);

    // When
    LoginLockedException exception =
        assertThrows(LoginLockedException.class, () -> memberLoginService.login(command));

    // Then
    assertEquals("로그인 시도 횟수를 초과했습니다. 비밀번호를 변경해주세요.", exception.getMessage());

    verify(memberRepository, never()).findByEmail(email);
    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(loginAttemptService, never()).recordFailure(email);
    verify(loginAttemptService, never()).resetFailures(email);
  }

  @Test
  @DisplayName("이메일에 해당하는 회원이 없으면 로그인에 실패한다")
  void throwsLoginFailedExceptionWhenMemberDoesNotExist() {
    // Given
    String email = "test@test.com";
    String password = "password1234";
    LoginCommand command = new LoginCommand(email, password);

    when(loginAttemptService.isLocked(email)).thenReturn(false);
    when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

    // When
    LoginFailedException exception =
        assertThrows(LoginFailedException.class, () -> memberLoginService.login(command));

    // Then
    assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.", exception.getMessage());

    verify(loginAttemptService, never()).recordFailure(email);
    verify(loginAttemptService, never()).resetFailures(email);
  }

  @Test
  @DisplayName("소셜 로그인 회원이 로컬 로그인을 시도하면 로그인에 실패한다")
  void throwsLoginFailedExceptionWhenMemberIsNotLocal() {
    // Given
    String email = "test@test.com";
    String password = "password1234";
    LoginCommand command = new LoginCommand(email, password);

    when(loginAttemptService.isLocked(email)).thenReturn(false);
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(member.getProvider()).thenReturn(MemberProvider.GOOGLE);

    // When
    LoginFailedException exception =
        assertThrows(LoginFailedException.class, () -> memberLoginService.login(command));

    // Then
    assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.", exception.getMessage());

    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(loginAttemptService, never()).recordFailure(email);
    verify(loginAttemptService, never()).resetFailures(email);
  }

  @Test
  @DisplayName("비활성 회원은 로그인에 실패한다")
  void throwsLoginFailedExceptionWhenMemberIsInactive() {
    // Given
    String email = "test@test.com";
    String password = "password1234";
    LoginCommand command = new LoginCommand(email, password);

    when(loginAttemptService.isLocked(email)).thenReturn(false);
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(member.getProvider()).thenReturn(MemberProvider.LOCAL);
    when(member.isActive()).thenReturn(false);

    // When
    LoginFailedException exception =
        assertThrows(LoginFailedException.class, () -> memberLoginService.login(command));

    // Then
    assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.", exception.getMessage());

    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(loginAttemptService, never()).recordFailure(email);
    verify(loginAttemptService, never()).resetFailures(email);
  }

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 실패 횟수를 누적하고 로그인에 실패한다")
  void recordsFailureWhenPasswordDoesNotMatch() {
    // Given
    String email = "test@test.com";
    String password = "wrong-password";
    String encodedPassword = "encoded-password";
    LoginCommand command = new LoginCommand(email, password);

    when(loginAttemptService.isLocked(email)).thenReturn(false);
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(member.getProvider()).thenReturn(MemberProvider.LOCAL);
    when(member.isActive()).thenReturn(true);
    when(member.getPassword()).thenReturn(encodedPassword);
    when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);
    when(loginAttemptService.recordFailure(email)).thenReturn(3L);

    // When
    LoginFailedException exception =
        assertThrows(LoginFailedException.class, () -> memberLoginService.login(command));

    // Then
    assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.", exception.getMessage());
    assertEquals(3L, exception.getFailureCount());

    verify(loginAttemptService).recordFailure(email);
    verify(loginAttemptService, never()).resetFailures(email);
  }

  @Test
  @DisplayName("5번째 비밀번호 실패 시 계정을 잠그고 로그인 잠금 예외를 발생시킨다")
  void throwsLoginLockedExceptionOnFifthPasswordFailure() {
    // Given
    String email = "test@test.com";
    String password = "wrong-password";
    String encodedPassword = "encoded-password";
    LoginCommand command = new LoginCommand(email, password);

    when(loginAttemptService.isLocked(email)).thenReturn(false);
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(member.getProvider()).thenReturn(MemberProvider.LOCAL);
    when(member.isActive()).thenReturn(true);
    when(member.getPassword()).thenReturn(encodedPassword);
    when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);
    when(loginAttemptService.recordFailure(email)).thenReturn(5L);

    // When
    LoginLockedException exception =
        assertThrows(LoginLockedException.class, () -> memberLoginService.login(command));

    // Then
    assertEquals("로그인 시도 횟수를 초과했습니다. 비밀번호를 변경해주세요.", exception.getMessage());

    verify(loginAttemptService).recordFailure(email);
    verify(loginAttemptService, never()).resetFailures(email);
  }
}
