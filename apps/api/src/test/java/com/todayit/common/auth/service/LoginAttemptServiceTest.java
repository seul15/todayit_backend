package com.todayit.common.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class LoginAttemptServiceTest {

  // Redis 대체 역할
  @Mock private StringRedisTemplate redisTemplate;

  // Redis의 String 값 연산
  @Mock private ValueOperations<String, String> valueOperations;

  private LoginAttemptService loginAttemptService;

  @BeforeEach
  void setUp() {
    loginAttemptService = new LoginAttemptService(redisTemplate);
  }

  @Test
  @DisplayName("첫 로그인 실패 시 실패 횟수를 1 증가시키고 잠금은 설정하지 않는다")
  void recordsFirstLoginFailureWithoutLocking() {
    // Given
    String email = "test@test.com";
    String key = "auth:login:attempt:" + email;
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(key)).thenReturn(1L);

    // When
    loginAttemptService.recordFailure(email);

    // Then
    verify(valueOperations).increment(key);
    verify(redisTemplate, never()).expire(key, Duration.ofHours(1));
  }

  @Test
  @DisplayName("로그인 실패가 5회가 되면 1시간 잠금을 설정한다")
  void locksLoginForOneHourOnFifthFailure() {
    // Given
    String email = "test@test.com";
    String key = "auth:login:attempt:" + email;
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(key)).thenReturn(5L);

    // When
    loginAttemptService.recordFailure(email);

    // Then
    verify(valueOperations).increment(key);
    verify(redisTemplate).expire(key, Duration.ofHours(1));
  }

  @Test
  @DisplayName("이미 잠금 기준을 넘은 실패에서는 잠금 시간을 다시 설정하지 않는다")
  void doesNotExtendLockAfterFifthFailure() {
    // Given
    String email = "test@test.com";
    String key = "auth:login:attempt:" + email;
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(key)).thenReturn(6L);

    // When
    loginAttemptService.recordFailure(email);

    // Then
    verify(valueOperations).increment(key);
    verify(redisTemplate, never()).expire(key, Duration.ofHours(1));
  }

  @Test
  @DisplayName("로그인 실패 기록이 없으면 잠긴 상태가 아니다")
  void returnsFalseWhenFailureRecordDoesNotExist() {
    // Given
    String email = "test@test.com";
    String key = "auth:login:attempt:" + email;
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(key)).thenReturn(null);

    // When
    boolean locked = loginAttemptService.isLocked(email);

    // Then
    assertFalse(locked);
  }

  @Test
  @DisplayName("로그인 실패 횟수가 5회면 잠긴 상태이다")
  void returnsTrueWhenFailureCountReachesMaximum() {
    // Given
    String email = "test@test.com";
    String key = "auth:login:attempt:" + email;
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(key)).thenReturn("5");

    // When
    boolean locked = loginAttemptService.isLocked(email);

    // Then
    assertTrue(locked);
  }

  @Test
  @DisplayName("로그인 실패 횟수가 4회면 아직 잠긴 상태가 아니다")
  void returnsFalseWhenFailureCountIsBelowMaximum() {
    // Given
    String email = "test@test.com";
    String key = "auth:login:attempt:" + email;
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(key)).thenReturn("4");

    // When
    boolean locked = loginAttemptService.isLocked(email);

    // Then
    assertFalse(locked);
  }

  @Test
  @DisplayName("로그인 성공 시 실패 기록을 삭제한다")
  void resetsFailureCountAfterSuccessfulLogin() {
    // Given
    String email = "test@test.com";
    String key = "auth:login:attempt:" + email;

    // When
    loginAttemptService.resetFailures(email);

    // Then
    verify(redisTemplate).delete(key);
  }
}
