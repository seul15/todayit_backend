package com.todayit.common.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Redis를 이용해 로그인 실패 횟수와 잠금 상태를 관리합니다. */
// 비밀번호 틀림 -> 실패 횟수 +1
// 실패 횟수 5회 이상 -> 로그인 잠금
// 로그인 성공 또는 비밀번호 변경 -> 실패 기록 삭제 -> 잠금 해제
@Service
public class LoginAttemptService {

  // 로그인 실패 횟수
  private static final int MAX_FAILURE_COUNT = 5;
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
   * 로그인 실패 횟수를 1 증가시킵니다.
   *
   * @param email 로그인에 실패한 회원 이메일
   * @return 증가된 로그인 실패 횟수
   */
  public long recordFailure(String email) {
    // 회원 이메일로 Redis Key 생성
    String key = KEY_PREFIX + email;

    // 기존 로그인 실패 횟수 +1
    Long failureCount = redisTemplate.opsForValue().increment(key);

    // 증가된 실패 횟수 반환
    return failureCount == null ? 0 : failureCount;
  }

  /**
   * 로그인 실패 횟수가 최대 횟수에 도달했는지 확인합니다.
   *
   * @param email 확인할 회원 이메일
   * @return 잠금 상태이면 true
   */
  public boolean isLocked(String email) {
    // 회원 이메일로 Redis Key 생성
    String key = KEY_PREFIX + email;

    // 현재 로그인 실패 횟수 조회
    String failureCount = redisTemplate.opsForValue().get(key);

    // 실패 기록이 없으면 잠기지 않은 상태
    if (failureCount == null) {
      return false;
    }
    // 실패 횟수가 5회 이상이면 계정 잠금
    return Long.parseLong(failureCount) >= MAX_FAILURE_COUNT;
  }

  /**
   * 로그인 성공 또는 비밀번호 변경 시 실패 기록을 초기화합니다.
   *
   * @param email 로그인에 성공한 회원 이메일
   */
  public void resetFailures(String email) {
    // 실패 기록 삭제 -> 계정 잠금 해제
    String key = KEY_PREFIX + email;
    redisTemplate.delete(key);
  }
}
