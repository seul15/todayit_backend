package com.todayit.member.client.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.RemoteKeySourceException;
import com.todayit.member.client.external.GoogleOAuthException.Reason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class GoogleOAuthClientTest {

  private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";

  /**
   * Google 서버의 HTTP 응답을 가짜로 반환하는 테스트 클라이언트를 생성합니다.
   *
   * @param status Google 서버가 반환할 HTTP 상태
   * @return 테스트용 Google OAuth 클라이언트
   */
  private GoogleOAuthClient createClient(HttpStatus status) {

    // 실제 Google 서버에 접속하지 않고 지정한 HTTP 응답을 반환
    WebClient.Builder builder =
        WebClient.builder()
            .exchangeFunction(request -> Mono.just(ClientResponse.create(status).build()));

    return new GoogleOAuthClient(builder, "test-client-id", "test-client-secret", REDIRECT_URI);
  }

  @Test
  @DisplayName("Google이 429를 반환하면 외부 서비스 장애로 처리한다")
  void handlesTooManyRequests() {

    // Given
    GoogleOAuthClient client = createClient(HttpStatus.TOO_MANY_REQUESTS);

    // When
    GoogleOAuthException exception =
        assertThrows(
            GoogleOAuthException.class,
            () -> client.authenticate("authorization-code", REDIRECT_URI));

    // Then
    assertEquals(Reason.PROVIDER_UNAVAILABLE, exception.getReason());
  }

  @Test
  @DisplayName("Google이 400을 반환하면 잘못된 인가 코드로 처리한다")
  void handlesBadAuthorizationCode() {

    // Given
    GoogleOAuthClient client = createClient(HttpStatus.BAD_REQUEST);

    // When
    GoogleOAuthException exception =
        assertThrows(
            GoogleOAuthException.class, () -> client.authenticate("invalid-code", REDIRECT_URI));

    // Then
    assertEquals(Reason.INVALID_AUTHORIZATION_CODE, exception.getReason());
  }

  @Test
  @DisplayName("Google 공개키 조회 실패를 외부 서비스 장애로 구분한다")
  void detectsJwkSetRetrievalFailure() {

    // Given
    // Spring Security가 공개키 조회 실패를 JwtException으로 감싼 상황
    JwtException exception =
        new JwtException(
            "JWT 검증 실패",
            new RemoteKeySourceException("Google 공개키 조회 실패", new RuntimeException("연결 시간 초과")));

    // When
    boolean result = GoogleOAuthClient.isJwkSetRetrievalFailure(exception);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("일반 JWT 검증 오류는 공개키 조회 장애로 분류하지 않는다")
  void doesNotClassifyInvalidJwtAsProviderFailure() {

    // Given
    JwtException exception = new BadJwtException("JWT 서명이 올바르지 않습니다.");

    // When
    boolean result = GoogleOAuthClient.isJwkSetRetrievalFailure(exception);

    // Then
    assertFalse(result);
  }
}
