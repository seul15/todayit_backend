package com.todayit.member.service;

import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.exception.EmailVerificationCodeExpiredException;
import com.todayit.member.exception.EmailVerificationResendTooSoonException;
import com.todayit.member.exception.InvalidEmailVerificationCodeException;
import com.todayit.member.exception.InvalidEmailVerificationTokenException;
import com.todayit.member.service.model.EmailVerificationConfirmResult;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 이메일 인증번호의 발급과 유효 상태를 관리합니다. */
@Service
public class EmailVerificationService {
  // 인증번호는 발급 후 30분 동안 유효
  private static final Duration CODE_TTL = Duration.ofMinutes(30);
  // 인증번호 재발송은 1분 후부터 가능
  private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
  // 인증 번호 입력 최대 실패 횟수
  private static final long MAX_FAILURE_COUNT = 3L;
  // 실제 인증번호를 저장하는 Redis Key prefix
  private static final String CODE_KEY_PREFIX = "auth:email-verification:code:";
  // 인증번호 재발송 제한을 저장하는 Redis Key prefix
  private static final String RESEND_KEY_PREFIX = "auth:email-verification:resend:";
  // 이메일 인증 완료 토큰은 30분 동안 유효
  private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofMinutes(30);
  // 인증번호 입력 실패 횟수를 저장하는 Redis Key prefix
  private static final String ATTEMPT_KEY_PREFIX = "auth:email-verification:attempt:";
  // 이메일 인증 완료 토큰을 저장하는 Redis Key prefix
  private static final String TOKEN_KEY_PREFIX = "auth:email-verification:token:";

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
    // 이전 인증번호의 실패 횟수 제거
    String attemptKey = ATTEMPT_KEY_PREFIX + keySuffix;

    redisTemplate.delete(attemptKey);

    return verificationCode;
  }

  /**
   * 입력한 이메일 인증번호를 확인합니다.
   *
   * @param email 인증할 이메일
   * @param purpose 인증 목적
   * @param verificationCode 사용자가 입력한 인증번호
   * @return 인증 성공 또는 새 인증번호 재발급 결과
   * @throws EmailVerificationCodeExpiredException 저장된 인증번호가 존재하지 않는 경우
   * @throws InvalidEmailVerificationCodeException 인증번호가 틀렸고 실패 횟수가 3회 미만인 경우
   */
  public EmailVerificationConfirmResult confirmCode(
      String email, EmailVerificationPurpose purpose, String verificationCode) {

    String keySuffix = createKeySuffix(email, purpose);

    String codeKey = CODE_KEY_PREFIX + keySuffix;

    String storedCode = redisTemplate.opsForValue().get(codeKey);

    // Key가 없으면 인증번호가 발급되지 않았거나 30분의 유효기간이 지난 것으로 처리한다.
    if (storedCode == null) {
      throw new EmailVerificationCodeExpiredException();
    }

    // 인증번호가 일치하면 일회성 인증 완료 토큰을 발급하고 기존 인증 상태를 정리한다.
    if (storedCode.equals(verificationCode)) {
      String verificationToken = completeVerification(email, purpose);

      return EmailVerificationConfirmResult.verified(verificationToken);
    }

    return handleVerificationFailure(keySuffix);
  }

  /**
   * 이메일 인증 완료 토큰의 이메일과 인증 목적을 검증합니다.
   *
   * @param email 검증할 이메일
   * @param purpose 인증 목적
   * @param verificationToken 이메일 인증 완료 토큰
   * @throws InvalidEmailVerificationTokenException 토큰이 없거나 인증 정보가 일치하지 않는 경우
   */
  public void validateVerificationToken(
      String email, EmailVerificationPurpose purpose, String verificationToken) {

    String tokenKey = TOKEN_KEY_PREFIX + verificationToken;

    String storedVerification = redisTemplate.opsForValue().get(tokenKey);

    String expectedVerification = createKeySuffix(email, purpose);

    // Redis Key가 만료됐거나 토큰에 저장된 이메일·목적이 다르면 사용할 수 없다.
    if (!expectedVerification.equals(storedVerification)) {
      throw new InvalidEmailVerificationTokenException();
    }
  }

  private EmailVerificationConfirmResult handleVerificationFailure(String keySuffix) {

    String attemptKey = ATTEMPT_KEY_PREFIX + keySuffix;

    // Redis increment를 사용해 현재 실패 횟수를 증가시킨다.
    Long failureCount = redisTemplate.opsForValue().increment(attemptKey);

    // 인증번호가 만료된 뒤 실패 횟수만 남지 않도록 동일한 유효시간을 설정한다.
    redisTemplate.expire(attemptKey, CODE_TTL);

    if (failureCount != null && failureCount >= MAX_FAILURE_COUNT) {

      // 3회 실패 시 기존 인증번호를 새 번호로 덮어써 이전 번호를 즉시 무효화한다.
      String reissuedVerificationCode = createAndStoreVerificationCode(keySuffix);

      String resendKey = RESEND_KEY_PREFIX + keySuffix;

      // 자동 재발급 직후에도 사용자가 다시 수동 발급하지 못하도록 재발송 제한
      redisTemplate.opsForValue().set(resendKey, "1", RESEND_COOLDOWN);

      // 새 번호는 HTTP 응답용이 아니라 이후 메일 전송 계층으로 전달하기 위한 내부 결과다.
      return EmailVerificationConfirmResult.reissued(reissuedVerificationCode);
    }

    long currentFailureCount = failureCount == null ? 1L : failureCount;

    throw new InvalidEmailVerificationCodeException(currentFailureCount);
  }

  private String createAndStoreVerificationCode(String keySuffix) {

    // 앞자리가 0이어도 항상 6자리를 유지하기 위해 문자열로 생성한다.
    String verificationCode = String.format("%06d", secureRandom.nextInt(1_000_000));

    String codeKey = CODE_KEY_PREFIX + keySuffix;

    // 동일한 Key에 저장하므로 재발급 시 이전 인증번호는 자동으로 무효화된다.
    redisTemplate.opsForValue().set(codeKey, verificationCode, CODE_TTL);

    String attemptKey = ATTEMPT_KEY_PREFIX + keySuffix;

    // 새 인증번호가 발급되면 이전 인증번호의 실패 횟수 초기화
    redisTemplate.delete(attemptKey);

    return verificationCode;
  }

  /**
   * 이메일 인증 완료 상태를 저장하고 인증 토큰을 발급합니다.
   *
   * @param email 인증한 이메일
   * @param purpose 인증 목적
   * @return 회원가입에 사용할 이메일 인증 완료 토큰
   */
  private String completeVerification(String email, EmailVerificationPurpose purpose) {

    String keySuffix = createKeySuffix(email, purpose);

    String verificationToken = generateVerificationToken();

    String tokenKey = TOKEN_KEY_PREFIX + verificationToken;

    redisTemplate.opsForValue().set(tokenKey, keySuffix, VERIFICATION_TOKEN_TTL);

    // 인증이 완료됐으므로 해당 인증번호와 관련 상태 제거
    redisTemplate.delete(CODE_KEY_PREFIX + keySuffix);

    redisTemplate.delete(ATTEMPT_KEY_PREFIX + keySuffix);

    redisTemplate.delete(RESEND_KEY_PREFIX + keySuffix);

    return verificationToken;
  }

  private String createKeySuffix(String email, EmailVerificationPurpose purpose) {

    // 같은 이메일이라도 인증 목적이 다르면 Redis 상태가 서로 덮어써지지 않도록 목적을 포함한다.
    return purpose.name() + ":" + email;
  }

  /**
   * 이메일 인증 완료 여부를 증명할 임의의 토큰을 생성합니다.
   *
   * @return URL-safe Base64 형식의 인증 토큰
   */
  private String generateVerificationToken() {
    byte[] tokenBytes = new byte[32];

    secureRandom.nextBytes(tokenBytes);
    // URL이나 JSON으로 전달하기 편하도록 padding 없는 URL-safe Base64를 사용한다.
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }
}
