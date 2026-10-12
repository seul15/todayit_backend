package com.todayit.member.service;

import com.todayit.common.exception.BusinessException;
import com.todayit.member.client.external.ResendEmailClient;
import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.exception.EmailVerificationCodeExpiredException;
import com.todayit.member.exception.EmailVerificationResendTooSoonException;
import com.todayit.member.exception.InvalidEmailVerificationCodeException;
import com.todayit.member.exception.InvalidEmailVerificationTokenException;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.service.model.EmailVerificationConfirmResult;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
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
  // 첫 발송 시도부터 고정된 10분 동안 최대 5회 허용
  private static final Duration SEND_WINDOW = Duration.ofMinutes(10);
  private static final int MAX_SEND_COUNT = 5;

  // 5회 한도에 도달하면 30분 동안 추가 발송 차단
  private static final Duration SEND_BLOCK_DURATION = Duration.ofMinutes(30);

  private static final String SEND_COUNT_KEY_PREFIX = "auth:email-verification:send-count:";
  private static final String SEND_BLOCK_KEY_PREFIX = "auth:email-verification:send-block:";

  /**
   * 인증번호 조회와 삭제를 원자적으로 처리하는 Redis Lua 스크립트 동시에 여러 인증 요청이 들어와도 동일한 인증번호가 중복 사용되지 않도록 함 KEYS[1]은 인증번호
   * Redis 키, ARGV[1]은 사용자가 입력한 인증번호 인증번호가 일치하여 삭제에 성공하면 1, 그렇지 않으면 0을 반환
   */
  private static final RedisScript<Long> CONSUME_CODE_SCRIPT =
      RedisScript.of(
          """
                          if redis.call('GET',KEYS[1]) == ARGV[1] then
                            return redis.call('DEL',KEYS[1])
                          end
                          return 0
                          """,
          Long.class);

  /**
   * 인증메일 발송 제한을 원자적으로 검사합니다.
   *
   * <p>KEYS[1]: 10분 발송 횟수 KEYS[2]: 30분 발송 차단 KEYS[3]: 1분 재발송 제한
   *
   * <p>ARGV[1]: 집계 시간(ms) ARGV[2]: 최대 발송 횟수 ARGV[3]: 차단 시간(ms) ARGV[4]: 재발송 제한 시간(ms) ARGV[5]: 1분
   * 제한 검사 여부(1: 검사, 0: 생략)
   *
   * <p>-1: 30분 제한, -2: 1분 제한 양수: 허용된 발송 횟수
   */
  private static final RedisScript<Long> SEND_LIMIT_SCRIPT =
      RedisScript.of(
          """
                  -- 30분 발송 제한을 가장 먼저 확인
                  if redis.call('EXISTS', KEYS[2]) == 1 then
                    return -1
                  end

                  -- 일반 재발송일 때만 1분 제한 확인
                  if ARGV[5] == '1'
                      and redis.call('EXISTS', KEYS[3]) == 1 then
                    return -2
                  end

                  -- 발송 시도 횟수 증가
                  local count = redis.call('INCR', KEYS[1])

                  -- 첫 시도에서만 10분 집계 시간 설정
                  if count == 1 then
                    redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))
                  end

                  -- 5회 한도 도달 시 30분 제한 시작
                  if count >= tonumber(ARGV[2]) then
                    redis.call('SET', KEYS[2], '1', 'PX', tonumber(ARGV[3]))
                  end

                  -- 허용된 발송에 대해서만 1분 재발송 제한 설정
                  redis.call('SET', KEYS[3], '1', 'PX', tonumber(ARGV[4]))

                  return count
                  """,
          Long.class);

  private final StringRedisTemplate redisTemplate;
  private final SecureRandom secureRandom = new SecureRandom();
  private final ResendEmailClient resendEmailClient;

  /**
   * 이메일 인증 상태 저장소와 이메일 발송 Client를 받습니다.
   *
   * @param redisTemplate 문자열 기반 Redis 접근 객체
   * @param resendEmailClient Resend 이메일 발송 Client
   */
  public EmailVerificationService(
      StringRedisTemplate redisTemplate, ResendEmailClient resendEmailClient) {
    this.redisTemplate = redisTemplate;
    this.resendEmailClient = resendEmailClient;
  }

  /**
   * 이메일 인증번호를 생성하고 Redis에 저장합니다.
   *
   * @param email 인증할 이메일
   * @param purpose 인증 목적
   * @return 생성된 6자리 인증번호
   * @throws EmailVerificationResendTooSoonException 재발송 대기 시간이 지나지 않은 경우
   * @throws BusinessException 인증 이메일 발송에 실패한 경우
   */
  public String issueCode(String email, EmailVerificationPurpose purpose) {
    String keySuffix = createKeySuffix(email, purpose);

    // 발송 제한 확인 및 횟수 집계
    recordEmailSendAttempt(email, keySuffix, true);

    // 인증번호 생성, 저장 및 실패 횟수 초기화
    String verificationCode = createAndStoreVerificationCode(keySuffix);

    sendVerificationEmail(email, verificationCode, keySuffix);

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
      // 인증번호 확인과 삭제를 원자적으로 실행하여 동시에 들어온 중복 인증 요청을 방지
      Long consumed =
          redisTemplate.execute(CONSUME_CODE_SCRIPT, List.of(codeKey), verificationCode);

      // 다른 요청이 먼저 인증번호를 사용했거나 이미 삭제된 경우 인증 실패 처리
      if (!Long.valueOf(1L).equals(consumed)) {
        throw new EmailVerificationCodeExpiredException();
      }

      // 인증번호 사용이 완료됐으므로 회원가입에 사용할 인증 완료 토큰 발급
      String verificationToken = completeVerification(email, purpose);

      return EmailVerificationConfirmResult.verified(verificationToken);
    }

    return handleVerificationFailure(email, keySuffix);
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

  /**
   * 사용이 완료된 이메일 인증 토큰을 무효화합니다.
   *
   * @param verificationToken 무효화할 이메일 인증 완료 토큰
   */
  public void invalidateVerificationToken(String verificationToken) {
    redisTemplate.delete(TOKEN_KEY_PREFIX + verificationToken);
  }

  private EmailVerificationConfirmResult handleVerificationFailure(String email, String keySuffix) {

    String attemptKey = ATTEMPT_KEY_PREFIX + keySuffix;

    // Redis increment를 사용해 현재 실패 횟수를 증가시킨다.
    Long failureCount = redisTemplate.opsForValue().increment(attemptKey);

    // 인증번호가 만료된 뒤 실패 횟수만 남지 않도록 동일한 유효시간을 설정한다.
    redisTemplate.expire(attemptKey, CODE_TTL);

    // 최대 실패 횟수 도달
    if (failureCount != null && failureCount >= MAX_FAILURE_COUNT) {
      // 3회 실패한 기존 인증번호는 자동 재발급 가능 여부와 관계없이 무효화
      redisTemplate.delete(CODE_KEY_PREFIX + keySuffix);

      // 30분 발송 제한 중이면 새 인증번호를 발급하지 않음
      recordEmailSendAttempt(email, keySuffix, false);

      String reissuedVerificationCode = createAndStoreVerificationCode(keySuffix);

      // 이메일 전송
      sendVerificationEmail(email, reissuedVerificationCode, keySuffix);

      // 새 번호는 HTTP 응답용이 아니라 이후 메일 전송 계층으로 전달하기 위한 내부 결과다.
      return EmailVerificationConfirmResult.reissued();
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

  /**
   * 인증번호 유효 시간을 초 단위로 반환합니다.
   *
   * @return 인증번호 유효 시간(초)
   */
  public long getCodeExpirationSeconds() {
    return CODE_TTL.toSeconds();
  }

  /**
   * 인증번호 재발급 대기 시간을 초 단위로 반환합니다.
   *
   * @return 재발급 대기 시간(초)
   */
  public long getResendCooldownSeconds() {
    return RESEND_COOLDOWN.toSeconds();
  }

  /**
   * 이메일 인증 완료 토큰의 유효 시간을 초 단위로 반환합니다.
   *
   * @return 인증 완료 토큰 유효 시간(초)
   */
  public long getVerificationTokenExpirationSeconds() {
    return VERIFICATION_TOKEN_TTL.toSeconds();
  }

  private void sendVerificationEmail(String email, String verificationCode, String keySuffix) {

    try {
      resendEmailClient.sendVerificationCode(email, verificationCode);
    } catch (RuntimeException exception) {
      // 이메일이 실제로 전달되지 않았다면 바로 다시 요청할 수 있도록 인증 상태 제거
      redisTemplate.delete(CODE_KEY_PREFIX + keySuffix);
      redisTemplate.delete(ATTEMPT_KEY_PREFIX + keySuffix);
      redisTemplate.delete(RESEND_KEY_PREFIX + keySuffix);

      throw new BusinessException(MemberErrorCode.EMAIL_DELIVERY_FAILED);
    }
  }

  /**
   * 이메일 인증메일 발송 가능 여부를 확인하고 발송 시도를 집계합니다.
   *
   * @param email 발송 대상 이메일
   * @param keySuffix 인증 목적과 이메일을 결합한 Redis 키
   * @param checkCooldown 1분 재발송 제한 검사 여부
   */
  private void recordEmailSendAttempt(String email, String keySuffix, boolean checkCooldown) {

    String countKey = SEND_COUNT_KEY_PREFIX + email;
    String blockKey = SEND_BLOCK_KEY_PREFIX + email;
    String resendKey = RESEND_KEY_PREFIX + keySuffix;

    Long result =
        redisTemplate.execute(
            SEND_LIMIT_SCRIPT,
            List.of(countKey, blockKey, resendKey),
            String.valueOf(SEND_WINDOW.toMillis()),
            String.valueOf(MAX_SEND_COUNT),
            String.valueOf(SEND_BLOCK_DURATION.toMillis()),
            String.valueOf(RESEND_COOLDOWN.toMillis()),
            checkCooldown ? "1" : "0");

    if (result == null) {
      throw new IllegalStateException("이메일 발송 제한 정보를 확인할 수 없습니다.");
    }

    // 30분 제한을 1분 재발송 제한보다 우선 처리
    if (result == -1L) {
      throw new BusinessException(MemberErrorCode.EMAIL_VERIFICATION_SEND_LIMIT_EXCEEDED);
    }

    if (result == -2L) {
      throw new EmailVerificationResendTooSoonException();
    }
  }
}
