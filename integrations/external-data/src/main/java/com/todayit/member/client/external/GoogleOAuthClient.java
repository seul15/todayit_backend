package com.todayit.member.client.external;

import com.todayit.member.client.external.GoogleOAuthException.Reason;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Google OAuth 인가 코드 교환과 ID Token 검증을 담당합니다. */
public class GoogleOAuthClient {

  private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

  private static final String JWK_SET_URL = "https://www.googleapis.com/oauth2/v3/certs";

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private final WebClient webClient;
  private final JwtDecoder jwtDecoder;
  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;

  /**
   * Google OAuth 클라이언트를 생성합니다.
   *
   * @param webClientBuilder HTTP 클라이언트 빌더
   * @param clientId Google OAuth Client ID
   * @param clientSecret Google OAuth Client Secret
   * @param redirectUri 서버에 등록된 Redirect URI
   */
  public GoogleOAuthClient(
      WebClient.Builder webClientBuilder,
      String clientId,
      String clientSecret,
      String redirectUri) {

    this.webClient = webClientBuilder.build();
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;

    // Google 공개키 조회에도 타임아웃 적용
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    requestFactory.setConnectTimeout(Duration.ofSeconds(3));
    requestFactory.setReadTimeout(REQUEST_TIMEOUT);

    this.jwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(JWK_SET_URL)
            .restOperations(new RestTemplate(requestFactory))
            .build();
  }

  /**
   * Google 인가 코드를 교환하고 사용자 정보를 검증합니다.
   *
   * @param authorizationCode Google 인가 코드
   * @param requestedRedirectUri 인증 요청에 사용한 Redirect URI
   * @return 검증된 Google 사용자 정보
   * @throws GoogleOAuthException Google 인증에 실패하거나 토큰이 유효하지 않은 경우
   * @throws IllegalStateException Google OAuth 환경변수가 설정되지 않은 경우
   */
  public GoogleIdentity authenticate(String authorizationCode, String requestedRedirectUri) {

    if (clientId.isBlank() || clientSecret.isBlank()) {
      throw new IllegalStateException("Google OAuth 설정이 없습니다.");
    }

    if (authorizationCode == null || authorizationCode.isBlank()) {
      throw new GoogleOAuthException(Reason.INVALID_AUTHORIZATION_CODE);
    }

    // Google Cloud에 등록한 URI와 요청 URI가 동일해야 함
    if (!redirectUri.equals(requestedRedirectUri)) {
      throw new GoogleOAuthException(Reason.INVALID_AUTHORIZATION_CODE);
    }

    String idToken = exchangeCode(authorizationCode);

    Jwt jwt;

    try {
      // Google 공개키로 서명 및 유효 시간 검증
      jwt = jwtDecoder.decode(idToken);
    } catch (JwtException exception) {
      throw new GoogleOAuthException(Reason.INVALID_ID_TOKEN);
    }

    // 토큰을 발급한 주체
    String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();

    // Google 발급자 확인
    if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
      throw new GoogleOAuthException(Reason.INVALID_ID_TOKEN);
    }

    // 토큰을 사용하도록 지정된 애플리케이션
    // 현재 Today-it OAuth Client에 발급된 토큰인지 확인
    List<String> audiences = jwt.getAudience();

    if (audiences == null || !audiences.contains(clientId)) {
      throw new GoogleOAuthException(Reason.INVALID_ID_TOKEN);
    }

    String authorizedParty = jwt.getClaimAsString("azp");

    if (authorizedParty != null && !clientId.equals(authorizedParty)) {
      throw new GoogleOAuthException(Reason.INVALID_ID_TOKEN);
    }

    // 토큰 만료 시각, 발급 시각 확인
    if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null) {
      throw new GoogleOAuthException(Reason.INVALID_ID_TOKEN);
    }

    // google 사용자 정보 추출 -> DB provider_user_id로 사용
    String providerUserId = jwt.getSubject();

    if (providerUserId == null || providerUserId.isBlank() || providerUserId.length() > 255) {
      throw new GoogleOAuthException(Reason.INVALID_ID_TOKEN);
    }

    String email = jwt.getClaimAsString("email");
    boolean emailVerified = Boolean.TRUE.equals(jwt.getClaim("email_verified"));

    return new GoogleIdentity(providerUserId, email, emailVerified);
  }

  /**
   * Google 인가 코드를 ID Token으로 교환합니다.
   *
   * @param authorizationCode Google 인가 코드
   * @return Google ID Token
   */
  private String exchangeCode(String authorizationCode) {

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

    form.add("code", authorizationCode);
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("redirect_uri", redirectUri);
    form.add("grant_type", "authorization_code");

    Map<String, Object> response;

    try {
      response =
          webClient
              .post()
              .uri(TOKEN_URL)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .bodyValue(form)
              .retrieve()
              .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
              .block(REQUEST_TIMEOUT);

    } catch (WebClientResponseException exception) {
      if (exception.getStatusCode().is4xxClientError()) {
        throw new GoogleOAuthException(Reason.INVALID_AUTHORIZATION_CODE);
      }

      throw new GoogleOAuthException(Reason.PROVIDER_UNAVAILABLE);

    } catch (WebClientRequestException | IllegalStateException exception) {
      throw new GoogleOAuthException(Reason.PROVIDER_UNAVAILABLE);
    }

    if (response == null
        || !(response.get("id_token") instanceof String idToken)
        || idToken.isBlank()) {
      throw new GoogleOAuthException(Reason.INVALID_ID_TOKEN);
    }

    return idToken;
  }
}
