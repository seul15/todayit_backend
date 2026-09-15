package com.todayit.common.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Refresh Token 생성, 저장, 조회, 삭제를 담당합니다. */
@Service
public class RefreshTokenService {

  // Refresh Token을 만들 때 사용할 랜덤 바이트 크기
  private static final int TOKEN_BYTE_LENGTH = 32;

  // Refresh Token 정보 저장 Key
  private static final String TOKEN_KEY_PREFIX = "auth:refresh:";

  // 회원별 Refresh Token 목록 저장 Key
  private static final String MEMBER_KEY_PREFIX = "auth:refresh:member:";

  private final StringRedisTemplate redisTemplate;
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Redis 접근 객체를 받습니다.
   *
   * @param redisTemplate Refresh Token을 저장할 Redis 접근 객체
   */
  public RefreshTokenService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
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

    // Refresh Token 원문은 저장하지 않고 해시값만 사용
    String tokenHash = hash(token);

    // Refresh Token으로 회원을 찾을 수 있도록 저장 -> 로그아웃할 때까지 유지하므로 만료 시간은 설정하지 않음
    redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + tokenHash, memberId);

    // 회원에게 발급된 Refresh Token 목록에도 추가 -> 비밀번호 변경/탈퇴 시 모든 로그인 세션을 찾기 위해 사용
    redisTemplate.opsForSet().add(MEMBER_KEY_PREFIX + memberId, tokenHash);

    // 실제 Refresh Token은 클라이언트에 반환
    return token;
  }

  /**
   * Refresh Token으로 로그인한 회원을 찾습니다.
   *
   * @param token 확인할 Refresh Token
   * @return Refresh Token에 연결된 회원 식별자
   */
  public Optional<String> findMemberId(String token) {

    String memberId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + hash(token));

    return Optional.ofNullable(memberId);
  }

  /**
   * 현재 기기의 Refresh Token을 삭제해 로그인 세션을 종료합니다.
   *
   * @param token 삭제할 Refresh Token
   */
  public void delete(String token) {

    String tokenHash = hash(token);
    String tokenKey = TOKEN_KEY_PREFIX + tokenHash;

    // Refresh Token이 어느 회원의 것인지 확인
    String memberId = redisTemplate.opsForValue().get(tokenKey);

    // 현재 기기의 Refresh Token만 삭제
    redisTemplate.delete(tokenKey);

    if (memberId != null) {
      // 회원별 로그인 세션 목록에서도 현재 Token만 제거
      redisTemplate.opsForSet().remove(MEMBER_KEY_PREFIX + memberId, tokenHash);
    }
  }

  /**
   * 회원에게 발급된 모든 Refresh Token을 삭제합니다. 비밀번호 변경이나 회원 탈퇴 시 모든 기기의 로그인 세션을 종료할 때 사용합니다.
   *
   * @param memberId 모든 로그인 세션을 종료할 회원 식별자
   */
  public void deleteAll(String memberId) {

    String memberKey = MEMBER_KEY_PREFIX + memberId;

    // 회원에게 발급된 모든 Refresh Token 해시 조회
    SetOperations<String, String> setOperations = redisTemplate.opsForSet();

    Set<String> tokenHashes = setOperations.members(memberKey);

    if (tokenHashes != null && !tokenHashes.isEmpty()) {
      // 회원의 모든 Refresh Token Key
      List<String> tokenKeys =
          tokenHashes.stream().map(tokenHash -> TOKEN_KEY_PREFIX + tokenHash).toList();

      // 모든 기기의 Refresh Token 삭제
      redisTemplate.delete(tokenKeys);
    }

    // 회원별 로그인 세션 목록도 삭제
    redisTemplate.delete(memberKey);
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
