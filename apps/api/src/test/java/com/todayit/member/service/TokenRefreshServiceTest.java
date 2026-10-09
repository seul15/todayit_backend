package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenService;
import com.todayit.common.exception.BusinessException;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.model.TokenRefreshResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenRefreshServiceTest {

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private MemberRepository memberRepository;

  @Mock private Member member;

  private TokenRefreshService tokenRefreshService;

  @BeforeEach
  void setUp() {
    tokenRefreshService =
        new TokenRefreshService(refreshTokenService, jwtTokenProvider, memberRepository);
  }

  @Test
  @DisplayName("유효한 Refresh Token이면 기존 세션으로 새 Access Token을 발급한다")
  void refreshesAccessTokenWithValidRefreshToken() {
    // Given
    String refreshToken = "refresh-token";
    String memberId = "member-1";
    String sessionId = "session-1";
    String accessToken = "new-access-token";
    List<String> roles = List.of("USER");
    long expiresIn = 1800L;

    when(refreshTokenService.findMemberId(refreshToken)).thenReturn(Optional.of(memberId));
    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
    when(member.isActive()).thenReturn(true);
    when(memberRepository.findRoleNamesByMemberId(memberId)).thenReturn(roles);
    when(refreshTokenService.getSessionId(refreshToken)).thenReturn(sessionId);
    when(jwtTokenProvider.createAccessToken(memberId, roles, sessionId)).thenReturn(accessToken);
    when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(expiresIn);

    // When
    TokenRefreshResult result = tokenRefreshService.refresh(refreshToken);

    // Then
    assertEquals(accessToken, result.accessToken());
    assertEquals(refreshToken, result.refreshToken());
    assertEquals(expiresIn, result.expiresIn());

    verify(refreshTokenService).findMemberId(refreshToken);
    verify(jwtTokenProvider).createAccessToken(memberId, roles, sessionId);
  }

  @Test
  @DisplayName("Redis에 없는 Refresh Token이면 토큰 갱신을 거부한다")
  void rejectsInvalidRefreshToken() {
    // Given
    String refreshToken = "invalid-refresh-token";

    when(refreshTokenService.findMemberId(refreshToken)).thenReturn(Optional.empty());

    // When
    BusinessException exception =
        assertThrows(BusinessException.class, () -> tokenRefreshService.refresh(refreshToken));

    // Then
    assertEquals(MemberErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());
  }
}
