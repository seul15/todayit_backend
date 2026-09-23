package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.exception.EmailVerificationCodeExpiredException;
import com.todayit.member.exception.EmailVerificationResendTooSoonException;
import com.todayit.member.exception.InvalidEmailVerificationCodeException;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.service.model.EmailVerificationConfirmResult;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationServiceTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  private EmailVerificationService emailVerificationService;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    emailVerificationService = new EmailVerificationService(redisTemplate);
  }

  @Test
  @DisplayName("이메일 인증번호를 발급하면 6자리 번호를 Redis에 30분 동안 저장한다")
  void issuesVerificationCode() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    String resendKey = "auth:email-verification:resend:SIGNUP:test@test.com";

    String codeKey = "auth:email-verification:code:SIGNUP:test@test.com";

    String attemptKey = "auth:email-verification:attempt:SIGNUP:test@test.com";

    when(valueOperations.setIfAbsent(resendKey, "1", Duration.ofMinutes(1))).thenReturn(true);

    // When
    String verificationCode = emailVerificationService.issueCode(email, purpose);

    // Then
    assertEquals(6, verificationCode.length());
    assertTrue(verificationCode.matches("\\d{6}"));

    verify(valueOperations).setIfAbsent(resendKey, "1", Duration.ofMinutes(1));

    verify(valueOperations).set(codeKey, verificationCode, Duration.ofMinutes(30));

    verify(redisTemplate).delete(attemptKey);
  }

  @Test
  @DisplayName("재발송 대기 시간이 지나지 않으면 인증번호를 다시 발급하지 않는다")
  void rejectsRequestDuringResendCooldown() {
    // Given
    String email = "test@test.com";
    EmailVerificationPurpose purpose = EmailVerificationPurpose.SIGNUP;

    String resendKey = "auth:email-verification:resend:SIGNUP:test@test.com";

    when(valueOperations.setIfAbsent(resendKey, "1", Duration.ofMinutes(1))).thenReturn(false);

    // When
    EmailVerificationResendTooSoonException exception =
        assertThrows(
            EmailVerificationResendTooSoonException.class,
            () -> emailVerificationService.issueCode(email, purpose));

    // Then
    assertEquals(MemberErrorCode.EMAIL_VERIFICATION_RESEND_TOO_SOON, exception.getErrorCode());

    verify(valueOperations, never()).set(anyString(), anyString(), eq(Duration.ofMinutes(30)));
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

    // When
    EmailVerificationConfirmResult result =
        emailVerificationService.confirmCode(email, purpose, verificationCode);

    // Then
    assertTrue(result.verified());
    assertNotNull(result.verificationToken());
    assertNull(result.reissuedVerificationCode());

    String tokenKey = "auth:email-verification:token:" + result.verificationToken();

    verify(valueOperations).set(tokenKey, keySuffix, Duration.ofMinutes(30));

    verify(redisTemplate).delete(codeKey);
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

    assertEquals(3, exception.getDetails().get("maxFailureCount"));

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

    String resendKey = "auth:email-verification:resend:" + keySuffix;

    when(valueOperations.get(codeKey)).thenReturn("123456");

    when(valueOperations.increment(attemptKey)).thenReturn(3L);

    // When
    EmailVerificationConfirmResult result =
        emailVerificationService.confirmCode(email, purpose, "999999");

    // Then
    assertFalse(result.verified());
    assertNull(result.verificationToken());
    assertNotNull(result.reissuedVerificationCode());

    assertEquals(6, result.reissuedVerificationCode().length());

    assertTrue(result.reissuedVerificationCode().matches("\\d{6}"));

    verify(valueOperations).set(codeKey, result.reissuedVerificationCode(), Duration.ofMinutes(30));

    verify(redisTemplate).delete(attemptKey);

    verify(valueOperations).set(resendKey, "1", Duration.ofMinutes(1));
  }
}
