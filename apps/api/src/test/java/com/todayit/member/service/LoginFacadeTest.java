package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenResult;
import com.todayit.common.auth.token.RefreshTokenService;
import com.todayit.member.service.command.LoginCommand;
import com.todayit.member.service.model.LoginResult;
import com.todayit.member.service.model.MemberLoginResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginFacadeTest {

  @Mock private MemberLoginService memberLoginService;

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private RefreshTokenService refreshTokenService;

  private LoginFacade loginFacade;

  @BeforeEach
  void setUp() {
    loginFacade = new LoginFacade(memberLoginService, jwtTokenProvider, refreshTokenService);
  }

  @Test
  @DisplayName("회원 인증에 성공하면 권한 목록을 포함한 Access Token과 Refresh Token을 발급한다")
  void issuesTokensAfterSuccessfulLogin() {
    // Given
    String email = "test@test.com";
    String password = "password";
    String memberId = "member-1";
    List<String> roles = List.of("DEV", "USER");
    String accessToken = "access-token";
    String refreshToken = "refresh-token";
    String sessionId = "session-1";
    long expiresIn = 1800L;

    LoginCommand command = new LoginCommand(email, password);
    MemberLoginResult memberLoginResult = new MemberLoginResult(memberId, roles);
    RefreshTokenResult refreshTokenResult = new RefreshTokenResult(refreshToken, sessionId);

    when(memberLoginService.login(command)).thenReturn(memberLoginResult);
    when(refreshTokenService.create(memberId)).thenReturn(refreshTokenResult);
    when(jwtTokenProvider.createAccessToken(memberId, roles, sessionId)).thenReturn(accessToken);
    when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(expiresIn);

    // When
    LoginResult result = loginFacade.login(command);

    // Then
    assertEquals(memberId, result.memberId());
    assertEquals(roles, result.roles());
    assertEquals(accessToken, result.accessToken());
    assertEquals(refreshToken, result.refreshToken());
    assertEquals(expiresIn, result.expiresIn());

    verify(memberLoginService).login(command);
    verify(refreshTokenService).create(memberId);
    verify(jwtTokenProvider).createAccessToken(memberId, roles, sessionId);
    verify(jwtTokenProvider).getAccessTokenExpirationSeconds();
  }
}
