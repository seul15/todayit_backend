package com.todayit.member.service;

import com.todayit.common.auth.jwt.JwtTokenProvider;
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
   * @param refreshTokenService Refresh Token 관리 Service
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
   * 유효한 Refresh Token으로 새 Access Token을 발급합니다.
   *
   * @param refreshToken 로그인 세션의 Refresh Token
   * @return 갱신된 토큰 정보
   * @throws BusinessException Refresh Token이 유효하지 않거나 회원이 비활성 상태일 때
   * @throws IllegalStateException 회원에게 유효한 권한이 없을 때
   */
  public TokenRefreshResult refresh(String refreshToken) {

    String memberId =
        refreshTokenService
            .findMemberId(refreshToken)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.REFRESH_TOKEN_INVALID));

    Member member =
        memberRepository
            .findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.REFRESH_TOKEN_INVALID));

    List<String> roles = memberRepository.findRoleNamesByMemberId(memberId);

    if (roles.isEmpty()) {
      throw new IllegalStateException("회원 권한 정보가 없습니다.");
    }

    String sessionId = refreshTokenService.getSessionId(refreshToken);

    String accessToken = jwtTokenProvider.createAccessToken(memberId, roles, sessionId);

    long expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

    return new TokenRefreshResult(accessToken, refreshToken, expiresIn);
  }
}
