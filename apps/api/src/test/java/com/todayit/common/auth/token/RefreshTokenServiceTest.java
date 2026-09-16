package com.todayit.common.auth.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  @Mock private SetOperations<String, String> setOperations;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    refreshTokenService = new RefreshTokenService(redisTemplate);
  }

  @Test
  @DisplayName("Refresh Token과 로그인 세션을 생성하고 Redis에 저장한다")
  void createsAndStoresRefreshToken() {
    // Given
    String memberId = "member-1";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(redisTemplate.opsForSet()).thenReturn(setOperations);

    // When
    RefreshTokenResult result = refreshTokenService.create(memberId);

    // Then
    assertFalse(result.token().isBlank());
    assertFalse(result.sessionId().isBlank());

    String expectedSessionId = hash(result.token());

    assertEquals(expectedSessionId, result.sessionId());

    String tokenKey = "auth:refresh:" + result.sessionId();

    String memberKey = "auth:refresh:member:" + memberId;

    verify(valueOperations).set(tokenKey, memberId);

    verify(setOperations).add(memberKey, result.sessionId());
  }

  @Test
  @DisplayName("Refresh Token으로 로그인한 회원 식별자를 조회한다")
  void findsMemberIdByRefreshToken() {
    // Given
    String refreshToken = "refresh-token";
    String memberId = "member-1";

    String tokenKey = "auth:refresh:" + hash(refreshToken);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(valueOperations.get(tokenKey)).thenReturn(memberId);

    // When
    Optional<String> result = refreshTokenService.findMemberId(refreshToken);

    // Then
    assertTrue(result.isPresent());
    assertEquals(memberId, result.get());
  }

  @Test
  @DisplayName("회원의 로그인 세션이 존재하면 유효한 세션으로 확인한다")
  void checksActiveLoginSession() {
    // Given
    String memberId = "member-1";
    String sessionId = "session-1";

    String sessionKey = "auth:refresh:" + sessionId;

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(valueOperations.get(sessionKey)).thenReturn(memberId);

    // When
    boolean active = refreshTokenService.isSessionActive(sessionId, memberId);

    // Then
    assertTrue(active);
  }

  @Test
  @DisplayName("다른 회원의 로그인 세션이면 유효하지 않은 세션으로 확인한다")
  void rejectsAnotherMembersLoginSession() {
    // Given
    String memberId = "member-1";
    String anotherMemberId = "member-2";
    String sessionId = "session-1";

    String sessionKey = "auth:refresh:" + sessionId;

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(valueOperations.get(sessionKey)).thenReturn(anotherMemberId);

    // When
    boolean active = refreshTokenService.isSessionActive(sessionId, memberId);

    // Then
    assertFalse(active);
  }

  @Test
  @DisplayName("Refresh Token을 삭제하면 현재 로그인 세션만 종료한다")
  void deletesCurrentRefreshToken() {
    // Given
    String refreshToken = "refresh-token";
    String memberId = "member-1";

    String sessionId = hash(refreshToken);
    String tokenKey = "auth:refresh:" + sessionId;

    String memberKey = "auth:refresh:member:" + memberId;

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(redisTemplate.opsForSet()).thenReturn(setOperations);

    when(valueOperations.get(tokenKey)).thenReturn(memberId);

    // When
    refreshTokenService.delete(refreshToken, memberId);

    // Then
    verify(redisTemplate).delete(tokenKey);

    verify(setOperations).remove(memberKey, sessionId);
  }

  @Test
  @DisplayName("회원의 모든 Refresh Token을 삭제하면 모든 로그인 세션을 종료한다")
  void deletesAllRefreshTokensByMemberId() {
    // Given
    String memberId = "member-1";
    String memberKey = "auth:refresh:member:" + memberId;

    Set<String> sessionIds = new LinkedHashSet<>(List.of("session-1", "session-2"));

    when(redisTemplate.opsForSet()).thenReturn(setOperations);

    when(setOperations.members(memberKey)).thenReturn(sessionIds);

    // When
    refreshTokenService.deleteAll(memberId);

    // Then
    verify(redisTemplate).delete(List.of("auth:refresh:session-1", "auth:refresh:session-2"));

    verify(redisTemplate).delete(memberKey);
  }

  @Test
  @DisplayName("다른 회원의 Refresh Token은 삭제하지 않는다")
  void doesNotDeleteAnotherMembersRefreshToken() {
    // Given
    String refreshToken = "refresh-token";
    String memberId = "member-1";
    String anotherMemberId = "member-2";

    String tokenHash = hash(refreshToken);
    String tokenKey = "auth:refresh:" + tokenHash;

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    when(valueOperations.get(tokenKey)).thenReturn(anotherMemberId);

    // When
    refreshTokenService.delete(refreshToken, memberId);

    // Then
    verify(redisTemplate, never()).delete(tokenKey);
  }

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);

    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
