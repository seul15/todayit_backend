package com.todayit.member.service;

import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.exception.EmailVerificationResendTooSoonException;
import java.security.SecureRandom;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 이메일 인증번호의 발급과 유효 상태를 관리합니다. */
@Service
public class EmailVerificationService {
  private static final Duration CODE_TTL = Duration.ofMinutes(30);
  private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

  private static final String CODE_KEY_PREFIX = "auth:email-verification:code:";

  private static final String RESEND_KEY_PREFIX = "auth:email-verification:resend:";

  private final StringRedisTemplate redisTemplate;
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * 이메일 인증 상태를 저장할 Redis 접근 객체를 받습니다.
   *
   * @param redisTemplate 문자열 기반 Redis 접근 객체
   */
  public EmailVerificationService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 이메일 인증번호를 생성하고 Redis에 저장합니다.
   *
   * @param email 인증할 이메일
   * @param purpose 인증 목적
   * @return 생성된 6자리 인증번호
   * @throws EmailVerificationResendTooSoonException 재발송 대기 시간이 지나지 않은 경우
   */
  public String issueCode(String email, EmailVerificationPurpose purpose) {
    // 예시 "SIGNUP:user@test.com"
    String keySuffix = purpose.name() + ":" + email;
    String resendKey = RESEND_KEY_PREFIX + keySuffix;

    // resendKey가 Redis에 없을때만 1 저장
    // auth:email-verification:resend:SIGNUP:user@test.com → "1"
    Boolean resendAllowed =
        redisTemplate.opsForValue().setIfAbsent(resendKey, "1", RESEND_COOLDOWN);

    if (!Boolean.TRUE.equals(resendAllowed)) {
      throw new EmailVerificationResendTooSoonException();
    }

    // 6자리 랜덤 숫자 생성
    String verificationCode = String.format("%06d", secureRandom.nextInt(1_000_000));

    String codeKey = CODE_KEY_PREFIX + keySuffix;

    // Redis에 인증번호 저장
    redisTemplate.opsForValue().set(codeKey, verificationCode, CODE_TTL);

    return verificationCode;
  }
}
