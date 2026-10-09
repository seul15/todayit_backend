package com.todayit.member.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayit.common.exception.GlobalExceptionHandler;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.service.SignupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

  @Mock private SignupService signupService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new MemberController(signupService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("사용 가능한 닉네임이면 available true를 반환한다")
  void returnsAvailableTrue() throws Exception {
    String nickname = "새닉네임";

    when(signupService.isNicknameAvailable(nickname)).thenReturn(true);

    mockMvc
        .perform(get("/api/v1/members/nickname/check").param("nickname", nickname))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.available").value(true));

    verify(signupService).isNicknameAvailable(nickname);
  }

  @Test
  @DisplayName("이미 사용 중인 닉네임이면 available false를 반환한다")
  void returnsAvailableFalse() throws Exception {
    String nickname = "더미사용자";

    when(signupService.isNicknameAvailable(nickname)).thenReturn(false);

    mockMvc
        .perform(get("/api/v1/members/nickname/check").param("nickname", nickname))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.available").value(false));

    verify(signupService).isNicknameAvailable(nickname);
  }

  @Test
  @DisplayName("닉네임 파라미터가 없으면 400을 반환한다")
  void rejectsMissingNickname() throws Exception {
    when(signupService.isNicknameAvailable(null))
        .thenThrow(new InvalidRequestException("닉네임은 필수입니다."));

    mockMvc
        .perform(get("/api/v1/members/nickname/check"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    verify(signupService).isNicknameAvailable(null);
  }

  @Test
  @DisplayName("닉네임에 허용되지 않는 문자가 있으면 400을 반환한다")
  void rejectsInvalidNickname() throws Exception {
    String nickname = "test!";

    when(signupService.isNicknameAvailable(nickname))
        .thenThrow(new InvalidRequestException("닉네임은 한글, 영문, 숫자만 사용할 수 있습니다."));

    mockMvc
        .perform(get("/api/v1/members/nickname/check").param("nickname", nickname))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    verify(signupService).isNicknameAvailable(nickname);
  }
}
