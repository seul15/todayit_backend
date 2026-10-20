package com.todayit.member.client.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.todayit.member.client.external.GoogleOAuthException.Reason;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class GoogleOAuthIdTokenTest {

  private static final String CLIENT_ID = "test-client-id";
  private static final String CLIENT_SECRET = "test-client-secret";
  private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";

  private KeyPair trustedKeys;

  @BeforeEach
  void setUp() throws Exception {

    // 테스트에서 사용할 RSA 공개키와 개인키 생성
    trustedKeys = generateKeyPair();
  }

  /**
   * RSA 키 쌍을 생성합니다.
   *
   * @return 테스트용 RSA 키 쌍
   */
  private KeyPair generateKeyPair() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  /**
   * 테스트용 Google ID Token을 생성하고 RSA 개인키로 서명합니다.
   *
   * @param signingKeys 서명에 사용할 키
   * @param issuer 토큰 발급자
   * @param audience 토큰 사용 대상
   * @param authorizedParty 승인된 클라이언트
   * @param expiresAt 토큰 만료 시간
   * @return RSA 서명이 포함된 JWT 문자열
   */
  private String signedIdToken(
      KeyPair signingKeys,
      String issuer,
      String audience,
      String authorizedParty,
      Instant expiresAt)
      throws JOSEException {

    JWTClaimsSet.Builder claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("google-user-123")
            .audience(audience)
            .issueTime(Date.from(Instant.now().minusSeconds(300)))
            .expirationTime(Date.from(expiresAt))
            .claim("email", "test@gmail.com")
            .claim("email_verified", true);

    if (authorizedParty != null) {
      claims.claim("azp", authorizedParty);
    }

    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());

    // RSA 개인키로 JWT 서명
    jwt.sign(new RSASSASigner((RSAPrivateKey) signingKeys.getPrivate()));

    return jwt.serialize();
  }

  /**
   * Google 토큰 발급 응답을 모의하고 실제 RSA 서명 검증기를 사용합니다.
   *
   * @param idToken 검증할 JWT
   * @return 테스트용 Google OAuth 클라이언트
   */
  private GoogleOAuthClient createClient(String idToken) {

    // 외부 Google 서버 대신 테스트용 응답 반환
    WebClient.Builder builder =
        WebClient.builder()
            .exchangeFunction(
                request ->
                    Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body("{\"id_token\":\"" + idToken + "\"}")
                            .build()));

    // 신뢰할 RSA 공개키로 JWT 서명과 만료 시간 검증
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey((RSAPublicKey) trustedKeys.getPublic()).build();

    return new GoogleOAuthClient(builder, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, decoder);
  }

  /**
   * 유효한 Google ID Token을 생성합니다.
   *
   * @return 유효한 JWT 문자열
   */
  private String validIdToken() throws JOSEException {
    return signedIdToken(
        trustedKeys,
        "https://accounts.google.com",
        CLIENT_ID,
        CLIENT_ID,
        Instant.now().plusSeconds(3600));
  }

  @Test
  @DisplayName("올바르게 서명된 Google ID Token은 인증에 성공한다")
  void authenticatesValidGoogleIdToken() throws Exception {

    // Given
    GoogleOAuthClient client = createClient(validIdToken());

    // When
    GoogleIdentity identity = client.authenticate("authorization-code", REDIRECT_URI);

    // Then
    assertEquals("google-user-123", identity.providerUserId());
    assertEquals("test@gmail.com", identity.email());
    assertEquals(true, identity.emailVerified());
  }

  @Test
  @DisplayName("Google 이외의 발급자가 포함된 토큰은 거부한다")
  void rejectsInvalidIssuer() throws Exception {

    // Given
    String token =
        signedIdToken(
            trustedKeys,
            "https://other-provider.example",
            CLIENT_ID,
            CLIENT_ID,
            Instant.now().plusSeconds(3600));

    // When
    GoogleOAuthException exception =
        assertThrows(
            GoogleOAuthException.class,
            () -> createClient(token).authenticate("authorization-code", REDIRECT_URI));

    // Then
    assertEquals(Reason.INVALID_ID_TOKEN, exception.getReason());
  }

  @Test
  @DisplayName("다른 클라이언트에 발급된 ID Token은 거부한다")
  void rejectsInvalidAudience() throws Exception {

    // Given
    String token =
        signedIdToken(
            trustedKeys,
            "https://accounts.google.com",
            "another-client-id",
            CLIENT_ID,
            Instant.now().plusSeconds(3600));

    // When
    GoogleOAuthException exception =
        assertThrows(
            GoogleOAuthException.class,
            () -> createClient(token).authenticate("authorization-code", REDIRECT_URI));

    // Then
    assertEquals(Reason.INVALID_ID_TOKEN, exception.getReason());
  }

  @Test
  @DisplayName("azp가 다른 Google Client ID이면 거부한다")
  void rejectsInvalidAuthorizedParty() throws Exception {

    // Given
    String token =
        signedIdToken(
            trustedKeys,
            "https://accounts.google.com",
            CLIENT_ID,
            "another-client-id",
            Instant.now().plusSeconds(3600));

    // When
    GoogleOAuthException exception =
        assertThrows(
            GoogleOAuthException.class,
            () -> createClient(token).authenticate("authorization-code", REDIRECT_URI));

    // Then
    assertEquals(Reason.INVALID_ID_TOKEN, exception.getReason());
  }

  @Test
  @DisplayName("만료된 Google ID Token은 거부한다")
  void rejectsExpiredToken() throws Exception {

    // Given
    // JWT 검증기의 허용 시간 오차보다 충분히 오래전에 만료
    String token =
        signedIdToken(
            trustedKeys,
            "https://accounts.google.com",
            CLIENT_ID,
            CLIENT_ID,
            Instant.now().minusSeconds(300));

    // When
    GoogleOAuthException exception =
        assertThrows(
            GoogleOAuthException.class,
            () -> createClient(token).authenticate("authorization-code", REDIRECT_URI));

    // Then
    assertEquals(Reason.INVALID_ID_TOKEN, exception.getReason());
  }

  @Test
  @DisplayName("신뢰하지 않는 개인키로 서명한 ID Token은 거부한다")
  void rejectsInvalidSignature() throws Exception {

    // Given
    // 공개키가 다른 별도의 RSA 키 쌍 생성
    KeyPair untrustedKeys = generateKeyPair();

    String token =
        signedIdToken(
            untrustedKeys,
            "https://accounts.google.com",
            CLIENT_ID,
            CLIENT_ID,
            Instant.now().plusSeconds(3600));

    // When
    GoogleOAuthException exception =
        assertThrows(
            GoogleOAuthException.class,
            () -> createClient(token).authenticate("authorization-code", REDIRECT_URI));

    // Then
    assertEquals(Reason.INVALID_ID_TOKEN, exception.getReason());
  }
}
