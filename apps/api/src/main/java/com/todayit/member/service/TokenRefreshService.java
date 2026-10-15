package com.todayit.member.service;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenResult;
import com.todayit.common.auth.token.RefreshTokenService;
import com.todayit.common.exception.BusinessException;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.model.TokenRefreshResult;
import java.util.List;
import org.springframework.stereotype.Service;

/** Refresh Token으로 Access Token을 갱신합니다. */
@Service
public class TokenRefreshService {

  private final RefreshTokenService refreshTokenService;
  private final JwtTokenProvider jwtTokenProvider;
  private final MemberRepository memberRepository;

  /**
   * 토큰 갱신에 필요한 서비스를 받습니다.
   *
   * @param refreshTokenService Refresh Token 검증 및 회전 Service
   * @param jwtTokenProvider Access Token 발급기
   * @param memberRepository 회원 Repository
   */
  public TokenRefreshService(
      RefreshTokenService refreshTokenService,
      JwtTokenProvider jwtTokenProvider,
      MemberRepository memberRepository) {
    this.refreshTokenService = refreshTokenService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.memberRepository = memberRepository;
  }

  /**
   * 유효한 Refresh Token을 새로운 Access Token과 Refresh Token으로 갱신합니다.
   *
   * <p>Refresh Token에 연결된 회원의 존재 여부와 활성 상태를 확인한 뒤, 회원 권한을 조회합니다. 이후 기존 Refresh Token을 무효화하고 새로운
   * Refresh Token을 발급합니다.
   *
   * <p>토큰 회전 시 기존 로그인 세션 ID를 유지하므로 사용자가 다시 로그인하지 않아도 인증 상태를 이어갈 수 있습니다.
   *
   * @param refreshToken 클라이언트가 전달한 기존 Refresh Token
   * @return 새 Access Token, 새 Refresh Token 및 Access Token 유효 시간
   * @throws BusinessException Refresh Token이 유효하지 않거나 회원이 비활성 상태인 경우
   * @throws IllegalStateException 회원에게 유효한 권한이 없는 경우
   */
  public TokenRefreshResult refresh(String refreshToken) {
    // Refresh Token으로 로그인한 회원 식별자 조회
    String memberId =
        refreshTokenService
            .findMemberId(refreshToken)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.REFRESH_TOKEN_INVALID));

    // 회원이 존재하고 활성 상태인지 확인
    // 탈퇴 또는 비활성화된 회원은 토큰 갱신을 허용하지 않음
    Member member =
        memberRepository
            .findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.REFRESH_TOKEN_INVALID));

    // 회원에게 부여된 권한 목록 조회
    List<String> roles = memberRepository.findRoleNamesByMemberId(memberId);

    // 유효한 권한이 없는 경우 Access Token을 발급하지 않음
    if (roles.isEmpty()) {
      throw new IllegalStateException("회원 권한 정보가 없습니다.");
    }

    // 기존 Refresh Token을 무효화하고 새 Refresh Token 발급
    // Redis Lua 스크립트로 토큰 검증과 교체를 원자적으로 처리
    RefreshTokenResult rotatedToken =
        refreshTokenService
            .rotate(refreshToken, memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.REFRESH_TOKEN_INVALID));

    // 기존 로그인 세션 ID를 유지하면서 새 Access Token 생성
    String accessToken =
        jwtTokenProvider.createAccessToken(memberId, roles, rotatedToken.sessionId());

    // 새로 발급된 Access Token의 유효 시간 조회
    long expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

    // 새로운 Access Token과 Refresh Token을 클라이언트에 반환
    return new TokenRefreshResult(accessToken, rotatedToken.token(), expiresIn);
  }
}
