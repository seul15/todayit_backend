package com.todayit.common.auth.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Redis를 이용해 로그인 실패 횟수와 임시 잠금 상태를 관리합니다. */
// recordFailure()로 실패 횟수 누적 -> 5회 도달 시 1시간 잠금
// isLocked()로 잠금 확인, 로그인 성공 시 resetFailures()로 실패 기록 초기화
@Service
public class LoginAttemptService {

  // 로그인 실패 횟수
  private static final int MAX_FAILURE_COUNT = 5;
  // 잠금 시간 -> 일단 임시로 1시간 설정
  private static final Duration LOCK_DURATION = Duration.ofHours(1);
  // 로그인 실패 기록용 Redis Key 접두사
  private static final String KEY_PREFIX = "auth:login:attempt:";
  // Redis에 로그인 실패 횟수와 잠금 상태 저장
  private final StringRedisTemplate redisTemplate;

  /**
   * 로그인 시도 상태를 저장할 Redis 접근 객체를 받습니다.
   *
   * @param redisTemplate 문자열 기반 Redis 접근 객체
   */
  public LoginAttemptService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 로그인 실패 횟수를 1 증가시킵니다. 실패 횟수가 최대 횟수에 도달하면 잠금 시간을 설정합니다.
   *
   * @param email 로그인에 실패한 회원 이메일
   */
  public void recordFailure(String email) {
    String key = KEY_PREFIX + email;

    Long failureCount = redisTemplate.opsForValue().increment(key);

    if (failureCount != null && failureCount == MAX_FAILURE_COUNT) {
      redisTemplate.expire(key, LOCK_DURATION);
    }
  }

  /**
   * 로그인 실패 횟수가 최대 횟수에 도달했는지 확인합니다.
   *
   * @param email 확인할 회원 이메일
   * @return 잠금 상태이면 true
   */
  public boolean isLocked(String email) {
    String key = KEY_PREFIX + email;

    String failureCount = redisTemplate.opsForValue().get(key);

    if (failureCount == null) {
      return false;
    }

    return Long.parseLong(failureCount) >= MAX_FAILURE_COUNT;
  }

  /**
   * 로그인 성공 시 실패 횟수와 잠금 상태를 초기화합니다.
   *
   * @param email 로그인에 성공한 회원 이메일
   */
  public void resetFailures(String email) {
    String key = KEY_PREFIX + email;
    redisTemplate.delete(key);
  }
}
