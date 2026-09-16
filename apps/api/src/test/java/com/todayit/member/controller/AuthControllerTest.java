package com.todayit.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayit.member.dto.request.LoginRequest;
import com.todayit.member.exception.LoginExceptionHandler;
import com.todayit.member.exception.LoginFailedException;
import com.todayit.member.exception.LoginLockedException;
import com.todayit.member.service.LoginFacade;
import com.todayit.member.service.LogoutService;
import com.todayit.member.service.model.LoginResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private LoginFacade loginFacade;

  @Mock private LogoutService logoutService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    AuthController authController = new AuthController(loginFacade, logoutService);

    mockMvc =
        MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new LoginExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("로그인에 성공하면 회원 정보와 Access Token, Refresh Token을 반환한다")
  void returnsTokensAfterSuccessfulLogin() throws Exception {
    // Given
    LoginRequest request = new LoginRequest("test@test.com", "password");

    LoginResult result =
        new LoginResult("member-1", "USER", "access-token", "refresh-token", 1800L);

    when(loginFacade.login(any())).thenReturn(result);

    // When
    ResultActions response =
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // Then
    response
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.expiresIn").value(1800))
        .andExpect(jsonPath("$.memberId").value("member-1"))
        .andExpect(jsonPath("$.role").value("USER"));

    verify(loginFacade).login(any());
  }

  @Test
  @DisplayName("로그인에 실패하면 현재 실패 횟수를 반환한다")
  void returnsFailureCountWhenLoginFails() throws Exception {
    // Given
    LoginRequest request = new LoginRequest("test@test.com", "wrong-password");

    when(loginFacade.login(any())).thenThrow(new LoginFailedException(3));

    // When
    ResultActions response =
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // Then
    response
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
        .andExpect(jsonPath("$.failureCount").value(3))
        .andExpect(jsonPath("$.maxFailureCount").value(5));
  }

  @Test
  @DisplayName("로그인 실패 횟수가 5회에 도달하면 잠금 응답을 반환한다")
  void returnsLockedResponseAfterFiveFailures() throws Exception {
    // Given
    LoginRequest request = new LoginRequest("test@test.com", "wrong-password");

    when(loginFacade.login(any())).thenThrow(new LoginLockedException());

    // When
    ResultActions response =
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // Then
    response
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"))
        .andExpect(jsonPath("$.failureCount").value(5))
        .andExpect(jsonPath("$.maxFailureCount").value(5))
        .andExpect(jsonPath("$.message").value("로그인 시도 횟수를 초과했습니다. 비밀번호를 변경해주세요."));
  }

  @Test
  @DisplayName("로그아웃하면 현재 기기의 로그인 세션을 종료한다")
  void logsOutCurrentSession() throws Exception {
    // Given
    String memberId = "member-1";
    String refreshToken = "refresh-token";

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(memberId, null);

    // When
    ResultActions response =
        mockMvc.perform(
            post("/api/v1/auth/logout")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                    {
                                      "refreshToken": "refresh-token"
                                    }
                                    """));

    // Then
    response.andExpect(status().isNoContent());

    verify(logoutService).logout(memberId, refreshToken);
  }
}
