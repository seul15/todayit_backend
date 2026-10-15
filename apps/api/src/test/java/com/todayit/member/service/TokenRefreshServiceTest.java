package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenResult;
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

/**
 * Refresh Token 회전 및 Access Token 갱신 처리를 검증합니다.
 *
 * <p>Refresh Token으로 회원을 조회하고 회원 활성 상태와 권한을 확인한 뒤, 토큰 회전 결과를 이용하여 새로운 Access Token을 발급하는 과정을
 * 테스트합니다.
 */
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
  @DisplayName("유효한 Refresh Token이면 두 토큰을 갱신하고 기존 세션을 유지한다")
  void refreshesAccessTokenWithRotatedRefreshToken() {

    // Given
    String oldRefreshToken = "old-refresh-token";
    String newRefreshToken = "new-refresh-token";
    String memberId = "member-1";
    String sessionId = "session-1";
    String newAccessToken = "new-access-token";

    List<String> roles = List.of("USER");
    long expiresIn = 1800L;

    // 기존 Refresh Token으로 회원 조회
    when(refreshTokenService.findMemberId(oldRefreshToken)).thenReturn(Optional.of(memberId));

    // 회원 존재 여부 및 활성 상태 확인
    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

    when(member.isActive()).thenReturn(true);

    // 회원 권한 조회
    when(memberRepository.findRoleNamesByMemberId(memberId)).thenReturn(roles);

    // Refresh Token 회전 성공
    // 세션 ID는 유지하고 Refresh Token만 변경
    when(refreshTokenService.rotate(oldRefreshToken, memberId))
        .thenReturn(Optional.of(new RefreshTokenResult(newRefreshToken, sessionId)));

    // 기존 세션 ID로 새로운 Access Token 발급
    when(jwtTokenProvider.createAccessToken(memberId, roles, sessionId)).thenReturn(newAccessToken);

    when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(expiresIn);

    // When
    TokenRefreshResult result = tokenRefreshService.refresh(oldRefreshToken);

    // Then
    assertEquals(newAccessToken, result.accessToken());
    assertEquals(newRefreshToken, result.refreshToken());
    assertNotEquals(oldRefreshToken, result.refreshToken());
    assertEquals(expiresIn, result.expiresIn());

    // 기존 Refresh Token을 회전했는지 확인
    verify(refreshTokenService).rotate(oldRefreshToken, memberId);

    // 기존 세션 ID를 사용해 Access Token을 발급했는지 확인
    verify(jwtTokenProvider).createAccessToken(memberId, roles, sessionId);
  }

  @Test
  @DisplayName("존재하지 않는 Refresh Token이면 갱신을 거부한다")
  void rejectsInvalidRefreshToken() {

    // Given
    String refreshToken = "invalid-refresh-token";

    // Redis에 Refresh Token 정보가 없는 경우
    when(refreshTokenService.findMemberId(refreshToken)).thenReturn(Optional.empty());

    // When
    BusinessException exception =
        assertThrows(BusinessException.class, () -> tokenRefreshService.refresh(refreshToken));

    // Then
    assertEquals(MemberErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());

    // 회원 조회 이후 처리는 실행하지 않음
    verifyNoInteractions(memberRepository, jwtTokenProvider);
  }

  @Test
  @DisplayName("비활성화된 회원은 토큰을 갱신할 수 없다")
  void rejectsInactiveMember() {

    // Given
    String refreshToken = "refresh-token";
    String memberId = "member-1";

    when(refreshTokenService.findMemberId(refreshToken)).thenReturn(Optional.of(memberId));

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

    // 비활성 회원
    when(member.isActive()).thenReturn(false);

    // When
    BusinessException exception =
        assertThrows(BusinessException.class, () -> tokenRefreshService.refresh(refreshToken));

    // Then
    assertEquals(MemberErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());

    // 비활성 회원의 Refresh Token은 회전하지 않음
    verify(refreshTokenService, never()).rotate(anyString(), anyString());

    verifyNoInteractions(jwtTokenProvider);
  }

  @Test
  @DisplayName("유효한 권한이 없는 회원은 토큰 갱신을 거부한다")
  void rejectsMemberWithoutRoles() {

    // Given
    String refreshToken = "refresh-token";
    String memberId = "member-1";

    when(refreshTokenService.findMemberId(refreshToken)).thenReturn(Optional.of(memberId));

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

    when(member.isActive()).thenReturn(true);

    // 회원에게 유효한 권한이 없는 경우
    when(memberRepository.findRoleNamesByMemberId(memberId)).thenReturn(List.of());

    // When & Then
    assertThrows(IllegalStateException.class, () -> tokenRefreshService.refresh(refreshToken));

    // 권한 검증에 실패했으므로 Refresh Token을 교체하지 않음
    verify(refreshTokenService, never()).rotate(anyString(), anyString());

    verifyNoInteractions(jwtTokenProvider);
  }

  @Test
  @DisplayName("동시 갱신으로 기존 토큰이 무효화되면 재발급을 거부한다")
  void rejectsConcurrentTokenRotation() {

    // Given
    String oldRefreshToken = "old-refresh-token";
    String memberId = "member-1";

    when(refreshTokenService.findMemberId(oldRefreshToken)).thenReturn(Optional.of(memberId));

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

    when(member.isActive()).thenReturn(true);

    when(memberRepository.findRoleNamesByMemberId(memberId)).thenReturn(List.of("USER"));

    // 다른 요청에서 이미 토큰을 회전한 상황
    when(refreshTokenService.rotate(oldRefreshToken, memberId)).thenReturn(Optional.empty());

    // When
    BusinessException exception =
        assertThrows(BusinessException.class, () -> tokenRefreshService.refresh(oldRefreshToken));

    // Then
    assertEquals(MemberErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());

    // 회전 실패 시 새로운 Access Token을 발급하지 않음
    verifyNoInteractions(jwtTokenProvider);
  }
}
