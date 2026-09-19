package com.todayit.common.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Access Token 생성과 검증을 담당합니다. */
@Component
public class JwtTokenProvider {

  // JWT 안에서 회원 권한 목록을 저장할 Key
  private static final String ROLES_CLAIM = "roles";

  // JWT 위조 여부를 확인하기 위한 서명 Key
  private final SecretKey signingKey;

  // Access Token 사용 가능 시간
  private final Duration accessTokenExpiration;

  // JWT 안에서 로그인 세션 식별자를 저장할 Key
  private static final String SESSION_ID_CLAIM = "sid";

  /**
   * JWT 설정값으로 토큰 서명 키와 만료 시간을 준비합니다.
   *
   * @param properties JWT 설정값
   */
  public JwtTokenProvider(JwtProperties properties) {
    // JWT_SECRET 문자열 -> 실제 JWT 서명에 사용할 Key로 변환
    this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    // Access Token 만료 시간 저장
    this.accessTokenExpiration = properties.accessTokenExpiration();
  }

  /**
   * 회원 식별자, 권한 목록, 로그인 세션으로 Access Token을 생성합니다.
   *
   * @param memberId 회원 식별자
   * @param roles 회원 권한 목록
   * @param sessionId 로그인 세션 식별자
   * @return 생성된 Access Token
   */
  public String createAccessToken(String memberId, List<String> roles, String sessionId) {

    // 토큰을 발급하는 현재 시간 확인
    Instant now = Instant.now();

    // 회원 정보와 로그인 세션을 JWT에 저장
    return Jwts.builder()
        .subject(memberId)
        .claim(ROLES_CLAIM, roles)
        .claim(SESSION_ID_CLAIM, sessionId)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(accessTokenExpiration)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Access Token에서 로그인 세션 식별자를 가져옵니다.
   *
   * @param token Access Token
   * @return 로그인 세션 식별자
   */
  public String getSessionId(String token) {
    return parseClaims(token).get(SESSION_ID_CLAIM, String.class);
  }

  /**
   * Access Token에서 회원 식별자를 가져옵니다.
   *
   * @param token Access Token
   * @return 회원 식별자
   */
  public String getMemberId(String token) {
    // JWT 내용 확인 -> subject에 저장된 회원 ID 반환
    return parseClaims(token).getSubject();
  }

  /**
   * Access Token에서 회원 권한 목록을 가져옵니다.
   *
   * @param token Access Token
   * @return 회원 권한 목록
   */
  public List<String> getRoles(String token) {
    Object rolesClaim = parseClaims(token).get(ROLES_CLAIM);

    if (!(rolesClaim instanceof List<?> roles)) {
      return List.of();
    }

    return roles.stream().filter(String.class::isInstance).map(String.class::cast).toList();
  }

  /**
   * Access Token의 서명과 만료 여부를 확인합니다.
   *
   * @param token 확인할 Access Token
   * @return 유효한 토큰이면 true
   */
  public boolean validateToken(String token) {
    try {
      // 서명과 만료 시간이 정상인지 확인
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException exception) {
      // 위조됐거나, 만료됐거나, JWT 형식이 잘못된 경우
      return false;
    }
  }

  /**
   * Access Token의 유효 시간을 초 단위로 반환합니다.
   *
   * @return Access Token 유효 시간
   */
  public long getAccessTokenExpirationSeconds() {
    return accessTokenExpiration.toSeconds();
  }

  // 서버의 Secret Key로 서명 확인 -> 정상이라면 JWT 안에 저장된 내용 반환
  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
