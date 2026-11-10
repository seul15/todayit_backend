package com.todayit.member.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.GlobalExceptionHandler;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.common.pagination.PageResult;
import com.todayit.member.entity.PreferredRegionCode;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.service.MemberProfileFacade;
import com.todayit.member.service.MemberProfileService;
import com.todayit.member.service.SignupService;
import com.todayit.member.service.command.MemberProfileCommand;
import com.todayit.member.service.model.MemberProfileDetailsResult;
import com.todayit.member.service.model.MemberProfileResult;
import com.todayit.place.entity.Category;
import com.todayit.place.service.PlaceScrapQueryService;
import com.todayit.place.service.model.PlaceResult;
import com.todayit.place.service.model.PlaceScrapSort;
import com.todayit.theme.Theme;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

  @Mock private PlaceScrapQueryService placeScrapQueryService;
  @Mock private SignupService signupService;
  @Mock private MemberProfileFacade memberProfileFacade;
  @Mock private MemberProfileService memberProfileService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new MemberController(
                    signupService,
                    placeScrapQueryService,
                    memberProfileFacade,
                    memberProfileService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("회원이 스크랩한 장소를 조회한다")
  void returnsScrappedPlaces() throws Exception {
    // Given
    PlaceResult place =
        new PlaceResult(
            1,
            "오늘의 식당",
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "서울특별시 종로구 종로 1",
            Category.RESTAURANT,
            10,
            List.of("https://example.com/place.jpg"),
            false);
    when(placeScrapQueryService.findScrappedPlaces("member-1", 1, 20, PlaceScrapSort.OLDEST))
        .thenReturn(new PageResult<>(List.of(place), 1, 20, 21));

    // When
    mockMvc
        .perform(
            get("/api/v1/members/me/scrapped-places")
                .param("page", "1")
                .param("sort", "OLDEST")
                .principal(new UsernamePasswordAuthenticationToken("member-1", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].placeId").value(1))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").value(21))
        .andExpect(jsonPath("$.data.totalPages").value(2));

    // Then
    verify(placeScrapQueryService).findScrappedPlaces("member-1", 1, 20, PlaceScrapSort.OLDEST);
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
  @DisplayName("프로필 이미지를 업로드하면 변경된 회원 정보를 반환한다")
  void uploadsProfileImage() throws Exception {
    // Given
    String memberId = "member-1";
    String profileImageUrl = "https://example.com/profile.png";
    LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 4, 5, 0);

    MockMultipartFile file =
        new MockMultipartFile("file", "profile.png", "image/png", new byte[] {1, 2, 3});

    MemberProfileResult result = new MemberProfileResult(memberId, profileImageUrl, updatedAt);

    when(memberProfileFacade.uploadProfileImage(eq(memberId), any(MultipartFile.class)))
        .thenReturn(result);

    // When & Then
    mockMvc
        .perform(
            multipart("/api/v1/members/me/profile-image")
                .file(file)
                .principal(new UsernamePasswordAuthenticationToken(memberId, null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.memberId").value(memberId))
        .andExpect(jsonPath("$.data.profileImage").value(profileImageUrl))
        .andExpect(jsonPath("$.data.updatedAt").value("2026-11-04T05:00:00"));

    verify(memberProfileFacade).uploadProfileImage(eq(memberId), any(MultipartFile.class));
  }

  @Test
  @DisplayName("프로필 이미지 약관에 동의하지 않으면 400을 반환한다")
  void rejectsProfileImageWithoutConsent() throws Exception {
    // Given
    String memberId = "member-1";

    MockMultipartFile file =
        new MockMultipartFile("file", "profile.png", "image/png", new byte[] {1, 2, 3});

    when(memberProfileFacade.uploadProfileImage(eq(memberId), any(MultipartFile.class)))
        .thenThrow(new BusinessException(MemberErrorCode.PROFILE_IMAGE_CONSENT_REQUIRED));

    // When & Then
    mockMvc
        .perform(
            multipart("/api/v1/members/me/profile-image")
                .file(file)
                .principal(new UsernamePasswordAuthenticationToken(memberId, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("PROFILE_IMAGE_CONSENT_REQUIRED"));
  }

  @Test
  @DisplayName("프로필 이미지 파일이 없으면 400을 반환한다")
  void rejectsMissingProfileImage() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/members/me/profile-image")
                .principal(new UsernamePasswordAuthenticationToken("member-1", null)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("회원 프로필 수정 요청을 처리하고 변경 결과를 반환한다")
  void updatesMemberProfile() throws Exception {
    // Given
    String memberId = "member-1";
    LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 9, 4, 0);

    MemberProfileDetailsResult result =
        new MemberProfileDetailsResult(
            memberId,
            "test@example.com",
            "새닉네임",
            null,
            List.of(new PreferredRegionCode("11", "110")),
            List.of(Theme.QUIET, Theme.ROMANTIC),
            updatedAt);

    when(memberProfileService.updateProfile(eq(memberId), any(MemberProfileCommand.class)))
        .thenReturn(result);

    // When & Then
    mockMvc
        .perform(
            patch("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                            {
                              "nickname": "새닉네임",
                              "preferredRegions": [
                                {
                                  "sidoCode": "11",
                                  "sigunguCode": "110"
                                }
                              ],
                              "preferredThemes": ["QUIET", "ROMANTIC"]
                            }
                            """)
                .principal(new UsernamePasswordAuthenticationToken(memberId, null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.memberId").value(memberId))
        .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
        .andExpect(jsonPath("$.data.preferredRegions[0].sidoCode").value("11"))
        .andExpect(jsonPath("$.data.preferredRegions[0].sigunguCode").value("110"))
        .andExpect(jsonPath("$.data.preferredThemes[0]").value("QUIET"))
        .andExpect(jsonPath("$.data.preferredThemes[1]").value("ROMANTIC"))
        .andExpect(jsonPath("$.data.updatedAt").value("2026-11-09T04:00:00"));

    verify(memberProfileService).updateProfile(eq(memberId), any(MemberProfileCommand.class));
  }

  @Test
  @DisplayName("수정 항목이 없으면 400을 반환한다")
  void rejectsEmptyMemberProfileRequest() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .principal(new UsernamePasswordAuthenticationToken("member-1", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("잘못된 테마 코드를 전달하면 400을 반환한다")
  void rejectsUnknownTheme() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preferredThemes\":[\"UNKNOWN_THEME\"]}")
                .principal(new UsernamePasswordAuthenticationToken("member-1", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("로그인한 회원 본인의 프로필 정보를 반환한다")
  void findsMemberProfile() throws Exception {
    // Given
    String memberId = "member-1";
    LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 9, 4, 0);

    MemberProfileDetailsResult result =
        new MemberProfileDetailsResult(
            memberId,
            "test@example.com",
            "테스트회원",
            null,
            List.of(new PreferredRegionCode("11", "110")),
            List.of(Theme.QUIET),
            updatedAt);

    when(memberProfileService.findProfile(memberId)).thenReturn(result);

    // When & Then
    mockMvc
        .perform(
            get("/api/v1/members/me")
                .principal(new UsernamePasswordAuthenticationToken(memberId, null)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.memberId").value(memberId))
        .andExpect(jsonPath("$.data.email").value("test@example.com"))
        .andExpect(jsonPath("$.data.nickname").value("테스트회원"))
        .andExpect(jsonPath("$.data.preferredRegions[0].sidoCode").value("11"))
        .andExpect(jsonPath("$.data.preferredRegions[0].sigunguCode").value("110"))
        .andExpect(jsonPath("$.data.preferredThemes[0]").value("QUIET"));

    verify(memberProfileService).findProfile(memberId);
  }

  @Test
  @DisplayName("프로필 이미지를 삭제하면 기본 이미지 상태를 반환한다")
  void deletesProfileImage() throws Exception {
    // Given
    String memberId = "member-1";
    LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 10, 10, 0);

    MemberProfileResult result = new MemberProfileResult(memberId, null, updatedAt);

    when(memberProfileFacade.deleteProfileImage(memberId)).thenReturn(result);

    // When & Then
    mockMvc
        .perform(
            delete("/api/v1/members/me/profile-image")
                .principal(new UsernamePasswordAuthenticationToken(memberId, null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.memberId").value(memberId))
        .andExpect(jsonPath("$.data.profileImage").value(nullValue()))
        .andExpect(jsonPath("$.data.updatedAt").value("2026-11-10T10:00:00"));

    verify(memberProfileFacade).deleteProfileImage(memberId);
  }

  @Test
  @DisplayName("선호 지역을 2개 선택하면 400을 반환한다")
  void rejectsMultiplePreferredRegions() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                    {
                                      "preferredRegions": [
                                        {
                                          "sidoCode": "11",
                                          "sigunguCode": "110"
                                        },
                                        {
                                          "sidoCode": "41",
                                          "sigunguCode": "135"
                                        }
                                      ]
                                    }
                                    """)
                .principal(new UsernamePasswordAuthenticationToken("member-1", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }
}
