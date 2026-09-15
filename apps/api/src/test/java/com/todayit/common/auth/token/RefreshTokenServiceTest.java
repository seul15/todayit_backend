package com.todayit.common.auth.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  private RefreshTokenService refreshTokenService;

  private Duration expiration;

  @BeforeEach
  void setUp() {
    expiration = Duration.ofDays(30);

    RefreshTokenProperties properties = new RefreshTokenProperties(expiration);

    refreshTokenService = new RefreshTokenService(redisTemplate, properties);
  }

  @Test
  @DisplayName("Refresh Token을 생성하고 Redis에 회원 정보를 저장한다")
  void createsAndStoresRefreshToken() {
    // Given
    String memberId = "member-1";
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // When
    String refreshToken = refreshTokenService.create(memberId);

    // Then
    assertFalse(refreshToken.isBlank());

    String key = "auth:refresh:" + hash(refreshToken);

    verify(valueOperations).set(key, memberId, expiration);
  }

  @Test
  @DisplayName("Refresh Token으로 로그인한 회원 식별자를 조회한다")
  void findsMemberIdByRefreshToken() {
    // Given
    String refreshToken = "refresh-token";
    String memberId = "member-1";
    String key = "auth:refresh:" + hash(refreshToken);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(key)).thenReturn(memberId);

    // When
    Optional<String> result = refreshTokenService.findMemberId(refreshToken);

    // Then
    assertTrue(result.isPresent());
    assertEquals(memberId, result.get());
  }

  @Test
  @DisplayName("Refresh Token을 삭제하면 로그인 세션을 종료한다")
  void deletesRefreshToken() {
    // Given
    String refreshToken = "refresh-token";
    String key = "auth:refresh:" + hash(refreshToken);

    // When
    refreshTokenService.delete(refreshToken);

    // Then
    verify(redisTemplate).delete(key);
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
