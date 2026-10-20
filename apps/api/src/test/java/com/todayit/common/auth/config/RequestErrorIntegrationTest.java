package com.todayit.common.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

/** 실제 서버의 입력 오류와 오류 디스패치가 인증 실패로 바뀌지 않는지 검증합니다. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestErrorIntegrationTest {

  @LocalServerPort private int port;
  @Autowired private JsonMapper jsonMapper;
  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private RefreshTokenService refreshTokenService;

  @Test
  @DisplayName("인증된 요청의 장소 식별자가 숫자가 아니면 400을 반환한다")
  void returnsBadRequestForInvalidPlaceIdWithValidAuthentication() throws Exception {
    // Given
    authenticate();
    String path = "/api/v1/places/not-a-number/scrap";

    // When
    HttpResponse<String> response = send(path, "POST", true);

    // Then
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(jsonMapper.readTree(response.body()).get("code").asText())
        .isEqualTo("INVALID_REQUEST");
  }

  @Test
  @DisplayName("인증된 요청의 장소 식별자가 치환되지 않은 변수이면 400을 반환한다")
  void returnsBadRequestForUnresolvedPlaceIdWithValidAuthentication() throws Exception {
    // Given
    authenticate();
    String path = "/api/v1/places/%7BplaceId%7D/scrap";

    // When
    HttpResponse<String> response = send(path, "POST", true);

    // Then
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(jsonMapper.readTree(response.body()).get("code").asText())
        .isEqualTo("INVALID_REQUEST");
  }

  @Test
  @DisplayName("공개 API의 페이지 번호가 숫자가 아니면 400을 반환한다")
  void returnsBadRequestForInvalidPageOnPublicEndpoint() throws Exception {
    // Given
    String path = "/api/v1/places?page=invalid";

    // When
    HttpResponse<String> response = send(path, "GET", false);

    // Then
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(jsonMapper.readTree(response.body()).get("code").asText())
        .isEqualTo("INVALID_REQUEST");
  }

  @Test
  @DisplayName("공개 API의 페이지 크기가 숫자가 아니면 400을 반환한다")
  void returnsBadRequestForInvalidSizeOnPublicEndpoint() throws Exception {
    // Given
    String path = "/api/v1/places?size=invalid";

    // When
    HttpResponse<String> response = send(path, "GET", false);

    // Then
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(jsonMapper.readTree(response.body()).get("code").asText())
        .isEqualTo("INVALID_REQUEST");
  }

  @Test
  @DisplayName("공개 API의 정렬 기준이 유효하지 않으면 400을 반환한다")
  void returnsBadRequestForInvalidSortOnPublicEndpoint() throws Exception {
    // Given
    String path = "/api/v1/places?sort=INVALID";

    // When
    HttpResponse<String> response = send(path, "GET", false);

    // Then
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(jsonMapper.readTree(response.body()).get("code").asText())
        .isEqualTo("INVALID_REQUEST");
  }

  @Test
  @DisplayName("인증된 요청에서 조회 경로에 POST를 보내면 405를 반환한다")
  void returnsMethodNotAllowedForPostToPlaceDetailWithValidAuthentication() throws Exception {
    // Given
    authenticate();
    String path = "/api/v1/places/scrap";

    // When
    HttpResponse<String> response = send(path, "POST", true);

    // Then
    assertThat(response.statusCode()).isEqualTo(405);
  }

  @Test
  @DisplayName("인증 없이 보호 API를 요청하면 입력값이 잘못되어도 401을 반환한다")
  void requiresAuthenticationForInvalidProtectedRequest() throws Exception {
    // Given
    String path = "/api/v1/places/invalid/scrap";

    // When
    HttpResponse<String> response = send(path, "POST", false);

    // Then
    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(jsonMapper.readTree(response.body()).get("code").asText())
        .isEqualTo("AUTHENTICATION_REQUIRED");
  }

  @Test
  @DisplayName("인증 없이 오류 경로를 직접 요청하면 401을 반환한다")
  void requiresAuthenticationForDirectErrorEndpointRequest() throws Exception {
    // Given
    String path = "/error";

    // When
    HttpResponse<String> response = send(path, "GET", false);

    // Then
    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(jsonMapper.readTree(response.body()).get("code").asText())
        .isEqualTo("AUTHENTICATION_REQUIRED");
  }

  @Test
  @DisplayName("로그인 요청 본문이 없으면 400을 반환한다")
  void returnsBadRequestForMissingLoginBody() throws Exception {
    // Given
    String path = "/api/v1/auth/login";

    // When
    HttpResponse<String> response = send(path, "POST", false);

    // Then
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(jsonMapper.readTree(response.body()).get("code").asText())
        .isEqualTo("INVALID_REQUEST");
  }

  private void authenticate() {
    when(jwtTokenProvider.validateToken("test-access-token")).thenReturn(true);
    when(jwtTokenProvider.getMemberId("test-access-token")).thenReturn("member-1");
    when(jwtTokenProvider.getSessionId("test-access-token")).thenReturn("session-1");
    when(jwtTokenProvider.getRoles("test-access-token")).thenReturn(List.of("USER"));
    when(refreshTokenService.isSessionActive("session-1", "member-1")).thenReturn(true);
  }

  private HttpResponse<String> send(String path, String method, boolean authenticated)
      throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.noBody());
    if (authenticated) {
      request.header("Authorization", "Bearer test-access-token");
    }
    try (HttpClient client = HttpClient.newHttpClient()) {
      return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
  }
}
