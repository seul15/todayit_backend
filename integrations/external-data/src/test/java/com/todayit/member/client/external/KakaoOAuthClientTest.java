package com.todayit.member.client.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.todayit.member.client.external.KakaoOAuthException.Reason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class KakaoOAuthClientTest {
  private static final String REDIRECT_URI = "http://localhost:3000/oauth/kakao/callback";

  private KakaoOAuthClient createClient(HttpStatus status, String responseBody) {

    WebClient.Builder builder =
        WebClient.builder()
            .exchangeFunction(
                request ->
                    Mono.just(
                        ClientResponse.create(status)
                            .header("Content-Type", "application/json")
                            .body(responseBody)
                            .build()));

    return new KakaoOAuthClient(builder, "test-rest-api-key", "test-secret", REDIRECT_URI);
  }

  @Test
  @DisplayName("유효한 인가 코드는 Access Token을 반환한다")
  void exchangesAuthorizationCode() {
    // Given
    KakaoOAuthClient client =
        createClient(
            HttpStatus.OK,
            """
                        {
                          "token_type": "bearer",
                          "access_token": "test-access-token"
                        }
                        """);

    // When
    String token = client.exchangeCode("valid-code", REDIRECT_URI);

    // Then
    assertEquals("test-access-token", token);
  }

  @Test
  @DisplayName("잘못된 인가 코드는 인증 실패로 처리한다")
  void rejectsInvalidAuthorizationCode() {
    KakaoOAuthClient client = createClient(HttpStatus.BAD_REQUEST, "{}");

    KakaoOAuthException exception =
        assertThrows(
            KakaoOAuthException.class, () -> client.exchangeCode("invalid-code", REDIRECT_URI));

    assertEquals(Reason.INVALID_AUTHORIZATION_CODE, exception.getReason());
  }

  @Test
  @DisplayName("요청 제한은 외부 서비스 장애로 처리한다")
  void handlesTooManyRequests() {
    KakaoOAuthClient client = createClient(HttpStatus.TOO_MANY_REQUESTS, "{}");

    KakaoOAuthException exception =
        assertThrows(KakaoOAuthException.class, () -> client.exchangeCode("code", REDIRECT_URI));

    assertEquals(Reason.PROVIDER_UNAVAILABLE, exception.getReason());
  }

  @Test
  @DisplayName("등록되지 않은 Redirect URI는 거부한다")
  void rejectsInvalidRedirectUri() {
    KakaoOAuthClient client = createClient(HttpStatus.OK, "{}");

    KakaoOAuthException exception =
        assertThrows(
            KakaoOAuthException.class,
            () -> client.exchangeCode("code", "http://localhost:3000/invalid"));

    assertEquals(Reason.INVALID_AUTHORIZATION_CODE, exception.getReason());
  }

  @Test
  @DisplayName("Access Token이 없는 응답은 거부한다")
  void rejectsMissingAccessToken() {
    KakaoOAuthClient client = createClient(HttpStatus.OK, "{\"token_type\":\"bearer\"}");

    KakaoOAuthException exception =
        assertThrows(KakaoOAuthException.class, () -> client.exchangeCode("code", REDIRECT_URI));

    assertEquals(Reason.PROVIDER_UNAVAILABLE, exception.getReason());
  }
}
