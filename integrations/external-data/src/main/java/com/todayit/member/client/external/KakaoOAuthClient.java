package com.todayit.member.client.external;

import com.todayit.member.client.external.KakaoOAuthException.Reason;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

/** Kakao OAuth 인가 코드를 Access Token으로 교환합니다. */
public class KakaoOAuthClient {

  private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private final WebClient webClient;
  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;

  /**
   * Kakao OAuth 클라이언트를 생성합니다.
   *
   * @param webClientBuilder HTTP 클라이언트 빌더
   * @param clientId Kakao REST API 키
   * @param clientSecret Kakao Client Secret
   * @param redirectUri 등록된 Redirect URI
   */
  public KakaoOAuthClient(
      WebClient.Builder webClientBuilder,
      String clientId,
      String clientSecret,
      String redirectUri) {
    this.webClient = webClientBuilder.build();
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
  }

  /**
   * 인가 코드를 Kakao Access Token으로 교환합니다.
   *
   * @param authorizationCode Kakao 인가 코드
   * @param requestedRedirectUri 인증 요청에 사용한 Redirect URI
   * @return Kakao Access Token
   * @throws KakaoOAuthException 인증 실패 또는 통신 오류
   * @throws IllegalStateException OAuth 환경변수가 없는 경우
   */
  public String exchangeCode(String authorizationCode, String requestedRedirectUri) {
    // Kakao OAuth 설정 확인
    if (clientId == null
        || clientId.isBlank()
        || clientSecret == null
        || clientSecret.isBlank()
        || redirectUri == null
        || redirectUri.isBlank()) {
      throw new IllegalStateException("Kakao OAuth 설정이 없습니다.");
    }

    // 인가 코드와 Redirect URI 검증
    if (authorizationCode == null
        || authorizationCode.isBlank()
        || !redirectUri.equals(requestedRedirectUri)) {
      throw new KakaoOAuthException(Reason.INVALID_AUTHORIZATION_CODE);
    }

    // Kakao 토큰 발급 요청 파라미터
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

    form.add("grant_type", "authorization_code");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("redirect_uri", redirectUri);
    form.add("code", authorizationCode);

    Map<String, Object> response =
        webClient
            .post()
            .uri(TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(form)
            .retrieve()
            .onStatus(
                status -> status.value() == 429,
                result -> Mono.error(new KakaoOAuthException(Reason.PROVIDER_UNAVAILABLE)))
            .onStatus(
                HttpStatusCode::is4xxClientError,
                result -> Mono.error(new KakaoOAuthException(Reason.INVALID_AUTHORIZATION_CODE)))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                result -> Mono.error(new KakaoOAuthException(Reason.PROVIDER_UNAVAILABLE)))
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(REQUEST_TIMEOUT)
            .onErrorMap(
                WebClientException.class,
                exception -> new KakaoOAuthException(Reason.PROVIDER_UNAVAILABLE, exception))
            .onErrorMap(
                CodecException.class,
                exception -> new KakaoOAuthException(Reason.PROVIDER_UNAVAILABLE, exception))
            .onErrorMap(
                TimeoutException.class,
                exception -> new KakaoOAuthException(Reason.PROVIDER_UNAVAILABLE, exception))
            .block();

    // 응답에 정상적인 Access Token이 있는지 확인
    if (response == null
        || !(response.get("access_token") instanceof String accessToken)
        || accessToken.isBlank()
        || !"bearer".equalsIgnoreCase(String.valueOf(response.get("token_type")))) {
      throw new KakaoOAuthException(Reason.PROVIDER_UNAVAILABLE);
    }

    return accessToken;
  }
}
