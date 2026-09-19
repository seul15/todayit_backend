package com.todayit.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayit.common.exception.GlobalExceptionHandler;
import com.todayit.member.dto.request.LoginRequest;
import com.todayit.member.exception.LoginFailedException;
import com.todayit.member.exception.LoginLockedException;
import com.todayit.member.service.LoginFacade;
import com.todayit.member.service.LogoutService;
import com.todayit.member.service.model.LoginResult;
import java.util.List;
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
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("로그인에 성공하면 회원 정보와 Access Token, Refresh Token을 반환한다")
  void returnsTokensAfterSuccessfulLogin() throws Exception {
    // Given
    LoginRequest request = new LoginRequest("test@test.com", "password");

    LoginResult result =
        new LoginResult("member-1", List.of("DEV", "USER"), "access-token", "refresh-token", 1800L);

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
        .andExpect(jsonPath("$.roles[0]").value("DEV"))
        .andExpect(jsonPath("$.roles[1]").value("USER"));

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

  @Test
  @DisplayName("로그인 이메일이 비어 있으면 400 응답을 반환한다")
  void returnsBadRequestWhenLoginEmailIsBlank() throws Exception {
    // Given
    LoginRequest request = new LoginRequest("", "password");

    // When
    ResultActions response =
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // Then
    response
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value("이메일은 필수입니다."));

    verify(loginFacade, never()).login(any());
  }

  @Test
  @DisplayName("로그아웃 Refresh Token이 비어 있으면 400 응답을 반환한다")
  void returnsBadRequestWhenRefreshTokenIsBlank() throws Exception {
    // Given
    String memberId = "member-1";

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
                                      "refreshToken": ""
                                    }
                                    """));

    // Then
    response
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value("Refresh Token은 필수입니다."));

    verify(logoutService, never()).logout(any(), any());
  }
}
