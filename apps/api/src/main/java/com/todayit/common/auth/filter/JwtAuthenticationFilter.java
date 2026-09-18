package com.todayit.common.auth.filter;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Access Token을 확인하고 로그인한 회원 정보를 Spring Security에 등록합니다. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;

  /**
   * JWT 검증기와 로그인 세션 관리 서비스를 받습니다.
   *
   * @param jwtTokenProvider Access Token 검증기
   * @param refreshTokenService 로그인 세션 관리 Service
   */
  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.refreshTokenService = refreshTokenService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Authorization Header 확인
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

    // Access Token이 없는 요청은 그대로 다음 단계로 이동
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Access Token만 추출
    String accessToken = authorization.substring(BEARER_PREFIX.length());

    // Access Token 서명과 만료 시간 확인
    if (!jwtTokenProvider.validateToken(accessToken)) {
      filterChain.doFilter(request, response);
      return;
    }

    // JWT에서 회원과 로그인 세션 정보 확인
    String memberId = jwtTokenProvider.getMemberId(accessToken);

    String sessionId = jwtTokenProvider.getSessionId(accessToken);

    if (memberId == null || sessionId == null) {
      filterChain.doFilter(request, response);
      return;
    }

    // Redis에 로그인 세션이 아직 존재하는지 확인
    // 로그아웃 / 비밀번호 변경 / 탈퇴로 삭제된 세션이면 인증하지 않음
    if (!refreshTokenService.isSessionActive(sessionId, memberId)) {
      filterChain.doFilter(request, response);
      return;
    }

    // 회원 권한 목록 확인
    List<String> roles = jwtTokenProvider.getRoles(accessToken);

    if (roles.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    // 각 권한을 Spring Security 권한으로 변환
    List<SimpleGrantedAuthority> authorities =
        roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();

    // Spring Security가 사용할 인증 정보 생성
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(memberId, null, authorities);

    // 현재 요청을 로그인한 사용자의 요청으로 등록
    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }
}
