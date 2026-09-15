package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.auth.jwt.JwtTokenProvider;
import com.todayit.common.auth.token.RefreshTokenService;
import com.todayit.member.service.command.LoginCommand;
import com.todayit.member.service.model.LoginResult;
import com.todayit.member.service.model.MemberLoginResult;
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
  @DisplayName("회원 인증에 성공하면 Access Token을 발급한다")
  void issuesAccessTokenAfterSuccessfulLogin() {
    // Given
    String email = "test@test.com";
    String password = "password";
    String memberId = "member-1";
    String role = "USER";
    String accessToken = "access-token";
    String refreshToken = "refresh-token";
    long expiresIn = 1800L;

    LoginCommand command = new LoginCommand(email, password);

    when(memberLoginService.login(command)).thenReturn(new MemberLoginResult(memberId, role));

    when(jwtTokenProvider.createAccessToken(memberId, role)).thenReturn(accessToken);

    when(refreshTokenService.create(memberId)).thenReturn(refreshToken);

    when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(expiresIn);

    // When
    LoginResult result = loginFacade.login(command);

    // Then
    assertEquals(memberId, result.memberId());
    assertEquals(role, result.role());
    assertEquals(accessToken, result.accessToken());
    assertEquals(refreshToken, result.refreshToken());
    assertEquals(expiresIn, result.expiresIn());

    verify(memberLoginService).login(command);
    verify(jwtTokenProvider).createAccessToken(memberId, role);
    verify(refreshTokenService).create(memberId);
    verify(jwtTokenProvider).getAccessTokenExpirationSeconds();
  }
}
