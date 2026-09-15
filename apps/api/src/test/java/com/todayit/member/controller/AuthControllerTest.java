package com.todayit.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayit.member.dto.request.LoginRequest;
import com.todayit.member.service.LoginFacade;
import com.todayit.member.service.model.LoginResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private LoginFacade loginFacade;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    AuthController authController = new AuthController(loginFacade);

    mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
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
}
