package com.todayit.common.auth.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Refresh Token 생성, 회전 및 로그인 세션 관리 기능을 검증합니다.
 *
 * <p>Redis Lua 스크립트의 실행 결과를 Mock으로 설정하여 성공, 실패 및 동시 갱신 충돌 시 Service 동작을 확인합니다.
 *
 * <p>실제 Redis의 원자적 처리 여부는 별도 통합 테스트에서 검증해야 합니다.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    refreshTokenService = new RefreshTokenService(redisTemplate);
  }

  @Test
  @DisplayName("Refresh Token과 독립적인 로그인 세션 ID를 생성한다")
  void createsRefreshTokenWithIndependentSessionId() {

    // Given
    String memberId = "member-1";

    // Redis Lua 스크립트에서 생성 성공을 반환
    when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(1L);

    // When
    RefreshTokenResult result = refreshTokenService.create(memberId);

    // Then
    assertFalse(result.token().isBlank());
    assertFalse(result.sessionId().isBlank());

    // Refresh Token 해시와 세션 ID는 서로 다른 값이어야 함
    assertNotEquals(hash(result.token()), result.sessionId());

    // 세션 ID는 UUID 형식으로 생성됨
    assertEquals(result.sessionId(), UUID.fromString(result.sessionId()).toString());

    // 세션, 토큰, 현재 토큰, 회원별 세션 정보의 저장 요청 검증
    verify(redisTemplate)
        .execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            eq(
                List.of(
                    "auth:refresh:" + result.sessionId(),
                    "auth:refresh:token:" + hash(result.token()),
                    "auth:refresh:current:" + result.sessionId(),
                    "auth:refresh:member:" + memberId)),
            eq(memberId),
            eq(result.sessionId()),
            eq(hash(result.token())));
  }

  @Test
  @DisplayName("Redis에서 로그인 세션 생성이 거부되면 예외를 발생시킨다")
  void rejectsFailedSessionCreation() {

    // Given
    when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(0L);

    // When & Then
    assertThrows(IllegalStateException.class, () -> refreshTokenService.create("member-1"));
  }

  @Test
  @DisplayName("Refresh Token에 연결된 회원 식별자를 조회한다")
  void findsMemberIdByRefreshToken() {

    // Given
    String token = "refresh-token";
    String sessionId = "session-1";
    String memberId = "member-1";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // Refresh Token 해시로 세션 ID 조회
    when(valueOperations.get("auth:refresh:token:" + hash(token))).thenReturn(sessionId);

    // 세션 ID로 회원 ID 조회
    when(valueOperations.get("auth:refresh:" + sessionId)).thenReturn(memberId);

    // When
    Optional<String> result = refreshTokenService.findMemberId(token);

    // Then
    assertEquals(Optional.of(memberId), result);
  }

  @Test
  @DisplayName("존재하지 않는 Refresh Token은 회원을 조회할 수 없다")
  void rejectsUnknownRefreshToken() {

    // Given
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // Redis에 토큰 조회 정보가 없으면 null 반환

    // When
    Optional<String> result = refreshTokenService.findMemberId("unknown-token");

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Refresh Token 회전 시 기존 세션 ID를 유지한다")
  void rotatesRefreshTokenWithSameSessionId() {

    // Given
    String oldToken = "old-refresh-token";
    String memberId = "member-1";
    String sessionId = "session-1";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // 기존 Refresh Token에 연결된 세션 조회
    when(valueOperations.get("auth:refresh:token:" + hash(oldToken))).thenReturn(sessionId);

    // Redis Lua 스크립트에서 토큰 회전 성공 반환
    when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(1L);

    // When
    RefreshTokenResult result = refreshTokenService.rotate(oldToken, memberId).orElseThrow();

    // Then
    assertNotEquals(oldToken, result.token());
    assertEquals(sessionId, result.sessionId());

    // 기존 토큰을 새 토큰으로 교체하는 Redis 작업 검증
    verify(redisTemplate)
        .execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            eq(
                List.of(
                    "auth:refresh:" + sessionId,
                    "auth:refresh:current:" + sessionId,
                    "auth:refresh:token:" + hash(oldToken),
                    "auth:refresh:token:" + hash(result.token()))),
            eq(memberId),
            eq(hash(oldToken)),
            eq(sessionId),
            eq(hash(result.token())));
  }

  @Test
  @DisplayName("이미 무효화된 Refresh Token은 회전할 수 없다")
  void rejectsAlreadyRotatedToken() {

    // Given
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // 회전이 끝난 기존 토큰은 Redis에서 조회되지 않음

    // When
    Optional<RefreshTokenResult> result = refreshTokenService.rotate("old-token", "member-1");

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("동시 갱신으로 Redis에서 회전이 거부되면 실패한다")
  void rejectsConcurrentRotation() {

    // Given
    String oldToken = "old-refresh-token";
    String sessionId = "session-1";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(valueOperations.get("auth:refresh:token:" + hash(oldToken))).thenReturn(sessionId);

    // 다른 요청이 먼저 회전한 경우 Lua 스크립트는 0 반환
    when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(0L);

    // When
    Optional<RefreshTokenResult> result = refreshTokenService.rotate(oldToken, "member-1");

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("로그인 세션이 유효하면 true를 반환한다")
  void checksActiveLoginSession() {

    // Given
    String memberId = "member-1";
    String sessionId = "session-1";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(valueOperations.get("auth:refresh:" + sessionId)).thenReturn(memberId);

    // When
    boolean active = refreshTokenService.isSessionActive(sessionId, memberId);

    // Then
    assertTrue(active);
  }

  @Test
  @DisplayName("다른 회원의 로그인 세션은 유효하지 않다")
  void rejectsAnotherMembersLoginSession() {

    // Given
    String memberId = "member-1";
    String sessionId = "session-1";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(valueOperations.get("auth:refresh:" + sessionId)).thenReturn("member-2");

    // When
    boolean active = refreshTokenService.isSessionActive(sessionId, memberId);

    // Then
    assertFalse(active);
  }

  @Test
  @DisplayName("로그아웃 시 현재 로그인 세션을 삭제한다")
  void deletesCurrentLoginSession() {

    // Given
    String token = "refresh-token";
    String memberId = "member-1";
    String sessionId = "session-1";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(valueOperations.get("auth:refresh:token:" + hash(token))).thenReturn(sessionId);

    // When
    refreshTokenService.delete(token, memberId);

    // Then
    // 현재 기기의 세션 및 토큰을 삭제하는 Lua 스크립트 호출
    verify(redisTemplate)
        .execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            eq(
                List.of(
                    "auth:refresh:" + sessionId,
                    "auth:refresh:current:" + sessionId,
                    "auth:refresh:token:" + hash(token),
                    "auth:refresh:member:" + memberId)),
            eq(memberId),
            eq(hash(token)),
            eq(sessionId));
  }

  @Test
  @DisplayName("회원의 모든 로그인 세션 삭제를 요청한다")
  void deletesAllLoginSessions() {

    // Given
    String memberId = "member-1";

    // When
    refreshTokenService.deleteAll(memberId);

    // Then
    // 회원별 세션 목록을 기준으로 전체 세션을 삭제하는 스크립트 호출
    verify(redisTemplate)
        .execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            eq(List.of("auth:refresh:member:" + memberId)),
            eq("auth:refresh:"),
            eq("auth:refresh:current:"),
            eq("auth:refresh:token:"));
  }

  /**
   * 테스트에서 Refresh Token 해시값을 계산합니다.
   *
   * <p>실제 Service와 동일한 SHA-256 알고리즘을 사용하여 Redis Key에 사용되는 토큰 해시를 검증합니다.
   *
   * @param token 해시를 생성할 Refresh Token
   * @return URL-safe Base64 형식의 SHA-256 해시값
   */
  private String hash(String token) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);

    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("테스트용 Refresh Token 해시 생성에 실패했습니다.", exception);
    }
  }
}
