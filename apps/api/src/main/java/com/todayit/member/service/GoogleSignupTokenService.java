package com.todayit.member.service;

import com.todayit.member.client.external.GoogleIdentity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 신규 Google 사용자의 회원가입용 임시 토큰을 관리합니다. */
@Service
public class GoogleSignupTokenService {

  private static final int TOKEN_BYTE_LENGTH = 32;
  private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
  private static final String TOKEN_KEY_PREFIX = "auth:oauth-signup:google:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * 임시 토큰 저장에 필요한 객체를 주입받습니다.
   *
   * @param redisTemplate Redis 접근 객체
   * @param objectMapper Google 인증 정보 직렬화 객체
   */
  public GoogleSignupTokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * 신규 Google 사용자에게 회원가입용 임시 토큰을 발급합니다.
   *
   * <p>토큰 원문은 Redis에 저장하지 않고 해시값을 Key로 사용합니다. Google 계정 정보는 JSON으로 저장하며 30분 후 자동 만료됩니다.
   *
   * @param identity 검증된 Google 계정 정보
   * @return 회원가입용 임시 토큰 원문
   * @throws IllegalArgumentException Google 계정 정보가 유효하지 않은 경우
   * @throws IllegalStateException 토큰 생성 또는 정보 직렬화에 실패한 경우
   */
  public String issue(GoogleIdentity identity) {
    if (identity == null
        || identity.providerUserId() == null
        || identity.providerUserId().isBlank()
        || identity.email() == null
        || identity.email().isBlank()
        || !identity.emailVerified()) {
      throw new IllegalArgumentException("유효한 Google 인증 정보가 필요합니다.");
    }

    byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(tokenBytes);

    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

    // 생성한 32바이트 난수를 sha-256으로 해싱
    String tokenHash = hash(token);
    String redisKey = TOKEN_KEY_PREFIX + tokenHash;

    String payload;

    try {
      payload = objectMapper.writeValueAsString(identity);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Google 회원가입 정보 직렬화에 실패했습니다.", exception);
    }

    redisTemplate.opsForValue().set(redisKey, payload, TOKEN_TTL);

    return token;
  }

  /**
   * 토큰을 SHA-256 해시로 변환합니다.
   *
   * @param token 해시할 토큰
   * @return URL-safe Base64 형식의 해시
   * @throws IllegalStateException SHA-256 알고리즘을 사용할 수 없는 경우
   */
  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("회원가입 토큰 해시 생성에 실패했습니다.", exception);
    }
  }
}
