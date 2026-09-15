package com.todayit.common.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Refresh Token 생성, 저장, 조회, 삭제를 담당합니다. */
@Service
public class RefreshTokenService {

  // Refresh Token을 만들 때 사용할 랜덤 바이트 크기
  private static final int TOKEN_BYTE_LENGTH = 32;

  // Redis에 Refresh Token을 저장할 Key 접두사
  private static final String KEY_PREFIX = "auth:refresh:";

  private final StringRedisTemplate redisTemplate;
  private final Duration expiration;
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Redis 접근 객체와 Refresh Token 유효 시간을 받습니다.
   *
   * @param redisTemplate Refresh Token을 저장할 Redis 접근 객체
   * @param properties Refresh Token 설정값
   */
  public RefreshTokenService(StringRedisTemplate redisTemplate, RefreshTokenProperties properties) {
    this.redisTemplate = redisTemplate;
    this.expiration = properties.expiration();
  }

  /**
   * 회원의 Refresh Token을 생성하고 Redis에 저장합니다.
   *
   * @param memberId 로그인한 회원 식별자
   * @return 생성된 Refresh Token
   */
  public String create(String memberId) {

    // 예측하기 어려운 랜덤 값으로 Refresh Token 생성
    byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(tokenBytes);

    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

    // Refresh Token 원문 대신 해시값을 Redis Key로 사용
    String key = KEY_PREFIX + hash(token);

    // Redis에 회원 ID 저장 -> Refresh Token이 누구의 로그인 세션인지 확인할 때 사용
    redisTemplate.opsForValue().set(key, memberId, expiration);

    // 실제 Refresh Token은 클라이언트에 리턴
    return token;
  }

  /**
   * Refresh Token으로 로그인한 회원을 찾습니다.
   *
   * @param token 확인할 Refresh Token
   * @return Refresh Token에 연결된 회원 식별자
   */
  public Optional<String> findMemberId(String token) {

    // Refresh Token 해시값으로 Redis에서 로그인 세션 조회
    String memberId = redisTemplate.opsForValue().get(KEY_PREFIX + hash(token));

    return Optional.ofNullable(memberId);
  }

  /**
   * Refresh Token을 삭제해 로그인 세션을 종료합니다.
   *
   * @param token 삭제할 Refresh Token
   */
  public void delete(String token) {

    // Redis에서 Refresh Token 삭제 -> 이후 Access Token 재발급 불가
    redisTemplate.delete(KEY_PREFIX + hash(token));
  }

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);

    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Refresh Token 해시 생성에 실패했습니다.", exception);
    }
  }
}
