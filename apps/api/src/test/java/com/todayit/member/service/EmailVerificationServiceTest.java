package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.BusinessException;
import com.todayit.member.client.external.ResendEmailClient;
import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.exception.EmailVerificationCodeExpiredException;
import com.todayit.member.exception.EmailVerificationResendTooSoonException;
import com.todayit.member.exception.InvalidEmailVerificationCodeException;
import com.todayit.member.exception.InvalidEmailVerificationTokenException;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.service.model.EmailVerificationConfirmResult;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationServiceTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private ResendEmailClient resendEmailClient;
  private EmailVerificationService emailVerificationService;

  private void mockSendLimitResult(
      String email, EmailVerificationPurpose purpose, boolean checkCooldown, long result) {

    String keySuffix = purpose.name() + ":" + email;

    when(redisTemplate.execute(
            any(),
            eq(
                List.of(
                    "auth:email-verification:send-count:" + email,
                    "auth:email-verification:send-block:" + email,
                    "auth:email-verification:resend:" + keySuffix)),
            eq("600000"),
            eq("5"),
            eq("1800000"),
            eq("60000"),
            eq(checkCooldown ? "1" : "0")))
        .thenReturn(result);
  }

  @BeforeEach
  void setUp() {
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    emailVerificationService =
        new EmailVerificationService(
            redisTemplate,
            resendEmailClient,
            Duration.ofMinutes(30),
            Duration.ofMinutes(1),
            3L,
            Duration.ofMinutes(30),
            Duration.ofMinutes(10),
            5,
            Duration.ofMinutes(30));
  }

  @Test
  @DisplayName("이메일 인증번호를 발급하면 6자리 번호를 Redis에 30분 동안 저장한다")
  void issuesVerificationCode() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    String codeKey = "auth:email-verification:code:SIGNUP:test@test.com";

    String attemptKey = "auth:email-verification:attempt:SIGNUP:test@test.com";

    mockSendLimitResult(email, purpose, true, 1L);

    // When
    String verificationCode = emailVerificationService.issueCode(email, purpose);

    // Then
    assertEquals(6, verificationCode.length());
    assertTrue(verificationCode.matches("\\d{6}"));

    verify(valueOperations).set(codeKey, verificationCode, Duration.ofMinutes(30));

    verify(redisTemplate).delete(attemptKey);

    verify(resendEmailClient).sendVerificationCode(email, verificationCode);
  }

  @Test
  @DisplayName("재발송 대기 시간이 지나지 않으면 인증번호를 다시 발급하지 않는다")
  void rejectsRequestDuringResendCooldown() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    // Lua 스크립트에서 1분 재발송 제한 확인
    mockSendLimitResult(email, purpose, true, -2L);

    // When
    EmailVerificationResendTooSoonException exception =
        assertThrows(
            EmailVerificationResendTooSoonException.class,
            () -> emailVerificationService.issueCode(email, purpose));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_RESEND_TOO_SOON, exception.getErrorCode());

    verify(valueOperations, never()).set(anyString(), anyString(), eq(Duration.ofMinutes(30)));

    verify(resendEmailClient, never()).sendVerificationCode(anyString(), anyString());
  }

  @Test
  @DisplayName("올바른 인증번호를 입력하면 이메일 인증 토큰을 발급한다")
  void confirmsValidVerificationCode() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;
    String verificationCode = "123456";

    String keySuffix = "SIGNUP:test@test.com";
    String codeKey = "auth:email-verification:code:" + keySuffix;
    String attemptKey = "auth:email-verification:attempt:" + keySuffix;
    String resendKey = "auth:email-verification:resend:" + keySuffix;

    when(valueOperations.get(codeKey)).thenReturn(verificationCode);

    // Lua 스크립트가 인증번호 확인 및 삭제에 성공한 상황
    when(redisTemplate.execute(any(), eq(List.of(codeKey)), eq(verificationCode))).thenReturn(1L);

    // When
    EmailVerificationConfirmResult result =
        emailVerificationService.confirmCode(email, purpose, verificationCode);

    // Then
    assertTrue(result.verified());
    assertNotNull(result.verificationToken());

    String tokenKey = "auth:email-verification:token:" + result.verificationToken();

    verify(redisTemplate).execute(any(), eq(List.of(codeKey)), eq(verificationCode));

    verify(valueOperations).set(tokenKey, keySuffix, Duration.ofMinutes(30));

    verify(redisTemplate).delete(attemptKey);
    verify(redisTemplate).delete(resendKey);
  }

  @Test
  @DisplayName("저장된 인증번호가 없으면 만료된 인증번호로 처리한다")
  void rejectsExpiredVerificationCode() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    String codeKey = "auth:email-verification:code:SIGNUP:test@test.com";

    when(valueOperations.get(codeKey)).thenReturn(null);

    // When
    EmailVerificationCodeExpiredException exception =
        assertThrows(
            EmailVerificationCodeExpiredException.class,
            () -> emailVerificationService.confirmCode(email, purpose, "123456"));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED, exception.getErrorCode());
  }

  @Test
  @DisplayName("잘못된 인증번호를 입력하면 실패 횟수를 증가시킨다")
  void incrementsFailureCountForInvalidCode() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    String codeKey = "auth:email-verification:code:SIGNUP:test@test.com";

    String attemptKey = "auth:email-verification:attempt:SIGNUP:test@test.com";

    when(valueOperations.get(codeKey)).thenReturn("123456");

    when(valueOperations.increment(attemptKey)).thenReturn(1L);

    // When
    InvalidEmailVerificationCodeException exception =
        assertThrows(
            InvalidEmailVerificationCodeException.class,
            () -> emailVerificationService.confirmCode(email, purpose, "999999"));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_CODE_INVALID, exception.getErrorCode());

    assertEquals(1L, exception.getDetails().get("failureCount"));

    assertEquals(3L, exception.getDetails().get("maxFailureCount"));

    verify(redisTemplate).expire(attemptKey, Duration.ofMinutes(30));
  }

  @Test
  @DisplayName("인증번호를 세 번 틀리면 새 인증번호를 발급하고 실패 횟수를 초기화한다")
  void reissuesCodeAfterThreeFailures() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    String keySuffix = "SIGNUP:test@test.com";

    String codeKey = "auth:email-verification:code:" + keySuffix;

    String attemptKey = "auth:email-verification:attempt:" + keySuffix;

    when(valueOperations.get(codeKey)).thenReturn("123456");

    when(valueOperations.increment(attemptKey)).thenReturn(3L);

    mockSendLimitResult(email, purpose, false, 2L);

    // When
    EmailVerificationConfirmResult result =
        emailVerificationService.confirmCode(email, purpose, "999999");

    // Then
    assertFalse(result.verified());
    assertNull(result.verificationToken());

    // Redis에 저장된 새 인증번호 확인
    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

    verify(valueOperations).set(eq(codeKey), codeCaptor.capture(), eq(Duration.ofMinutes(30)));

    String reissuedCode = codeCaptor.getValue();

    assertTrue(reissuedCode.matches("[0-9]{6}"));

    verify(redisTemplate).delete(attemptKey);

    verify(redisTemplate).delete(codeKey);

    verify(resendEmailClient).sendVerificationCode(email, reissuedCode);
  }

  @Test
  @DisplayName("3회 실패 시 발송 제한 중이어도 기존 인증번호를 무효화한다")
  void invalidatesCodeAfterThreeFailuresWhenSendBlocked() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    String codeKey = "auth:email-verification:code:SIGNUP:" + email;
    String attemptKey = "auth:email-verification:attempt:SIGNUP:" + email;

    when(valueOperations.get(codeKey)).thenReturn("123456");
    when(valueOperations.increment(attemptKey)).thenReturn(3L);

    // 30분 발송 차단 상태
    mockSendLimitResult(email, purpose, false, -1L);

    // When
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> emailVerificationService.confirmCode(email, purpose, "999999"));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_SEND_LIMIT_EXCEEDED, exception.getErrorCode());

    // 3회 실패한 기존 인증번호 삭제
    verify(redisTemplate).delete(codeKey);

    // 발송 차단 중에는 새 인증번호 저장 및 이메일 발송 금지
    verify(valueOperations, never()).set(eq(codeKey), anyString(), eq(Duration.ofMinutes(30)));

    verify(resendEmailClient, never()).sendVerificationCode(anyString(), anyString());
  }

  @Test
  @DisplayName("이메일과 인증 목적이 일치하는 인증 토큰은 검증에 성공한다")
  void validatesVerificationToken() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;
    String verificationToken = "verification-token";

    String tokenKey = "auth:email-verification:token:" + verificationToken;

    when(valueOperations.get(tokenKey)).thenReturn("SIGNUP:test@test.com");

    // When
    emailVerificationService.validateVerificationToken(email, purpose, verificationToken);

    // Then
    verify(valueOperations).get(tokenKey);
  }

  @Test
  @DisplayName("존재하지 않거나 만료된 인증 토큰은 검증에 실패한다")
  void rejectsExpiredVerificationToken() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;
    String verificationToken = "expired-token";

    String tokenKey = "auth:email-verification:token:" + verificationToken;

    when(valueOperations.get(tokenKey)).thenReturn(null);

    // When
    InvalidEmailVerificationTokenException exception =
        assertThrows(
            InvalidEmailVerificationTokenException.class,
            () ->
                emailVerificationService.validateVerificationToken(
                    email, purpose, verificationToken));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID, exception.getErrorCode());
  }

  @Test
  @DisplayName("다른 이메일로 발급된 인증 토큰은 검증에 실패한다")
  void rejectsVerificationTokenForDifferentEmail() {
    // Given
    String email = "other@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;
    String verificationToken = "verification-token";

    String tokenKey = "auth:email-verification:token:" + verificationToken;

    when(valueOperations.get(tokenKey)).thenReturn("SIGNUP:test@test.com");

    // When
    InvalidEmailVerificationTokenException exception =
        assertThrows(
            InvalidEmailVerificationTokenException.class,
            () ->
                emailVerificationService.validateVerificationToken(
                    email, purpose, verificationToken));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID, exception.getErrorCode());
  }

  @Test
  @DisplayName("이미 사용된 인증번호로는 인증 토큰을 다시 발급할 수 없다")
  void rejectsAlreadyConsumedVerificationCode() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;
    String verificationCode = "123456";

    String codeKey = "auth:email-verification:code:SIGNUP:" + email;

    when(valueOperations.get(codeKey)).thenReturn(verificationCode);

    // 다른 요청이 인증번호를 먼저 사용해서 Lua 스크립트가 0을 반환
    when(redisTemplate.execute(any(), eq(List.of(codeKey)), eq(verificationCode))).thenReturn(0L);

    // When
    EmailVerificationCodeExpiredException exception =
        assertThrows(
            EmailVerificationCodeExpiredException.class,
            () -> emailVerificationService.confirmCode(email, purpose, verificationCode));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED, exception.getErrorCode());
  }

  @Test
  @DisplayName("발송 횟수 제한에 도달하면 인증메일을 추가로 발송하지 않는다")
  void rejectsEmailSendWhenLimitExceeded() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    // Lua 스크립트에서 30분 발송 제한 확인
    mockSendLimitResult(email, purpose, true, -1L);

    // When
    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> emailVerificationService.issueCode(email, purpose));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_SEND_LIMIT_EXCEEDED, exception.getErrorCode());

    verify(resendEmailClient, never()).sendVerificationCode(anyString(), anyString());
  }
}
