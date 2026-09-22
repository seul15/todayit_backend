package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.exception.EmailVerificationResendTooSoonException;
import com.todayit.member.exception.MemberErrorCode;
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

    when(valueOperations.setIfAbsent(resendKey, "1", Duration.ofMinutes(1))).thenReturn(true);

    // When
    String verificationCode = emailVerificationService.issueCode(email, purpose);

    // Then
    assertEquals(6, verificationCode.length());
    assertTrue(verificationCode.matches("\\d{6}"));

    verify(valueOperations).setIfAbsent(resendKey, "1", Duration.ofMinutes(1));

    verify(valueOperations).set(codeKey, verificationCode, Duration.ofMinutes(30));
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
}
