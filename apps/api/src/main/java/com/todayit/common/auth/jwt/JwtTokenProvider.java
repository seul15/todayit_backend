package com.todayit.common.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Access Token 생성과 검증을 담당합니다. */
@Component
public class JwtTokenProvider {

  private static final String ROLE_CLAIM = "role";

  private final SecretKey signingKey;
  private final Duration accessTokenExpiration;

  /**
   * JWT 설정값으로 토큰 서명 키와 만료 시간을 준비합니다.
   *
   * @param properties JWT 설정값
   */
  public JwtTokenProvider(JwtProperties properties) {
    this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiration = properties.accessTokenExpiration();
  }

  /**
   * 회원 식별자와 권한으로 Access Token을 생성합니다.
   *
   * @param memberId 회원 식별자
   * @param role 회원 권한
   * @return 생성된 Access Token
   */
  public String createAccessToken(String memberId, String role) {
    Instant now = Instant.now();

    return Jwts.builder()
        .subject(memberId)
        .claim(ROLE_CLAIM, role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(accessTokenExpiration)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Access Token에서 회원 식별자를 가져옵니다.
   *
   * @param token Access Token
   * @return 회원 식별자
   */
  public String getMemberId(String token) {
    return parseClaims(token).getSubject();
  }

  /**
   * Access Token에서 회원 권한을 가져옵니다.
   *
   * @param token Access Token
   * @return 회원 권한
   */
  public String getRole(String token) {
    return parseClaims(token).get(ROLE_CLAIM, String.class);
  }

  /**
   * Access Token의 서명과 만료 여부를 확인합니다.
   *
   * @param token 확인할 Access Token
   * @return 유효한 토큰이면 true
   */
  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException exception) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
