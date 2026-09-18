package com.todayit.common.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenService;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private RefreshTokenService refreshTokenService;

  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, refreshTokenService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("유효한 Access Token의 모든 권한을 인증 정보에 등록한다")
  void authenticatesValidAccessTokenWithMultipleRoles() throws ServletException, IOException {
    // Given
    String accessToken = "access-token";
    String memberId = "member-1";
    String sessionId = "session-1";
    List<String> roles = List.of("DEV", "USER");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + accessToken);

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(jwtTokenProvider.validateToken(accessToken)).thenReturn(true);
    when(jwtTokenProvider.getMemberId(accessToken)).thenReturn(memberId);
    when(jwtTokenProvider.getSessionId(accessToken)).thenReturn(sessionId);
    when(refreshTokenService.isSessionActive(sessionId, memberId)).thenReturn(true);
    when(jwtTokenProvider.getRoles(accessToken)).thenReturn(roles);

    // When
    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // Then
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    assertEquals(memberId, authentication.getPrincipal());

    assertTrue(
        authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));

    assertTrue(
        authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_DEV")));

    verify(jwtTokenProvider).validateToken(accessToken);
    verify(refreshTokenService).isSessionActive(sessionId, memberId);
  }

  @Test
  @DisplayName("로그인 세션이 삭제된 Access Token은 인증하지 않는다")
  void rejectsAccessTokenWithDeletedSession() throws ServletException, IOException {
    // Given
    String accessToken = "access-token";
    String memberId = "member-1";
    String sessionId = "session-1";

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + accessToken);

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(jwtTokenProvider.validateToken(accessToken)).thenReturn(true);
    when(jwtTokenProvider.getMemberId(accessToken)).thenReturn(memberId);
    when(jwtTokenProvider.getSessionId(accessToken)).thenReturn(sessionId);
    when(refreshTokenService.isSessionActive(sessionId, memberId)).thenReturn(false);

    // When
    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // Then
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  @DisplayName("유효하지 않은 Access Token은 인증하지 않는다")
  void rejectsInvalidAccessToken() throws ServletException, IOException {
    // Given
    String accessToken = "invalid-access-token";

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + accessToken);

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(jwtTokenProvider.validateToken(accessToken)).thenReturn(false);

    // When
    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // Then
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
