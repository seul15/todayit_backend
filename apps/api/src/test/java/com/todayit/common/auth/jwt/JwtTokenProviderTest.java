package com.todayit.common.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    JwtProperties properties =
        new JwtProperties("01234567890123456789012345678901", Duration.ofMinutes(30));

    jwtTokenProvider = new JwtTokenProvider(properties);
  }

  @Test
  @DisplayName("회원 식별자와 여러 권한으로 Access Token을 생성한다")
  void createsAccessToken() {
    // Given
    String memberId = "member-1";
    List<String> roles = List.of("DEV", "USER");
    String sessionId = "session-1";

    // When
    String token = jwtTokenProvider.createAccessToken(memberId, roles, sessionId);

    // Then
    assertTrue(jwtTokenProvider.validateToken(token));
    assertEquals(memberId, jwtTokenProvider.getMemberId(token));
    assertEquals(roles, jwtTokenProvider.getRoles(token));
    assertEquals(sessionId, jwtTokenProvider.getSessionId(token));
  }

  @Test
  @DisplayName("올바르지 않은 Access Token은 유효하지 않다")
  void rejectsInvalidAccessToken() {
    // Given
    String invalidToken = "invalid-token";

    // When
    boolean valid = jwtTokenProvider.validateToken(invalidToken);

    // Then
    assertFalse(valid);
  }
}
