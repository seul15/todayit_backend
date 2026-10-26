package com.todayit.member.client.external;

import com.todayit.member.client.external.KakaoOAuthException.Reason;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

/** Kakao OAuth 인가 코드 교환과 사용자 정보 조회를 담당합니다. */
public class KakaoOAuthClient {

  private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";

  private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

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
                status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(),
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

  /**
   * Kakao Access Token을 사용하여 사용자 정보를 조회합니다.
   *
   * @param accessToken Kakao Access Token
   * @return Kakao 사용자 정보
   * @throws KakaoOAuthException 토큰이 잘못되었거나 사용자 정보 조회에 실패한 경우
   */
  public KakaoIdentity getUserInfo(String accessToken) {
    // Access Token 입력값 검증
    if (accessToken == null || accessToken.isBlank()) {
      throw new KakaoOAuthException(Reason.INVALID_ACCESS_TOKEN);
    }

    // Kakao 사용자 정보 조회 API 호출
    Map<String, Object> response =
        webClient
            .get()
            .uri(USER_INFO_URL)
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()

            // Access Token이 잘못되었거나 만료된 경우
            .onStatus(
                status -> status.value() == HttpStatus.UNAUTHORIZED.value(),
                result -> Mono.error(new KakaoOAuthException(Reason.INVALID_ACCESS_TOKEN)))

            // 요청 제한 및 Kakao 서버 장애
            .onStatus(
                status ->
                    status.value() == HttpStatus.TOO_MANY_REQUESTS.value()
                        || status.is5xxServerError(),
                result -> Mono.error(new KakaoOAuthException(Reason.PROVIDER_UNAVAILABLE)))

            // 그 외 잘못된 요청이나 권한 부족 등
            .onStatus(
                HttpStatusCode::is4xxClientError,
                result -> Mono.error(new KakaoOAuthException(Reason.PROVIDER_REQUEST_REJECTED)))

            // JSON 응답을 Map으로 변환
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})

            // 응답 대기 시간 제한
            .timeout(REQUEST_TIMEOUT)

            // 네트워크 및 응답 처리 오류
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

    // Kakao 회원 고유 ID 검증
    if (response == null
        || !(response.get("id") instanceof Number kakaoId)
        || kakaoId.longValue() <= 0) {
      throw new KakaoOAuthException(Reason.INVALID_USER_INFO);
    }

    String email = null;
    boolean emailVerified = false;

    // Kakao 계정 정보 추출
    Object kakaoAccount = response.get("kakao_account");

    if (kakaoAccount instanceof Map<?, ?> account) {

      // 이메일은 동의 및 계정 상태에 따라 없을 수 있음
      Object rawEmail = account.get("email");

      if (rawEmail instanceof String kakaoEmail && !kakaoEmail.isBlank()) {
        email = kakaoEmail;
      }

      // 이메일이 존재하고 인증 및 유효 여부가 모두 true일 때만 인정
      emailVerified =
          email != null
              && Boolean.TRUE.equals(account.get("is_email_verified"))
              && Boolean.TRUE.equals(account.get("is_email_valid"));
    }

    return new KakaoIdentity(String.valueOf(kakaoId.longValue()), email, emailVerified);
  }
}
