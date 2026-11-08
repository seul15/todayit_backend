package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.course.service.RegionQueryService;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.command.MemberProfileCommand;
import com.todayit.member.service.command.MemberProfileCommand.PreferredRegion;
import com.todayit.member.service.model.MemberProfileDetailsResult;
import com.todayit.member.service.model.MemberProfileResult;
import com.todayit.theme.Theme;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 회원 프로필 이미지 변경과 회원 상태 검증을 테스트합니다. */
@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

  private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";

  @Mock private MemberRepository memberRepository;
  @Mock private MemberAgreementRepository memberAgreementRepository;
  @Mock private SignupService signupService;
  @Mock private RegionQueryService regionQueryService;

  private MemberProfileService service;

  @BeforeEach
  void setUp() {
    service =
        new MemberProfileService(
            memberRepository, memberAgreementRepository, signupService, regionQueryService);
  }

  @Test
  @DisplayName("활성 회원의 프로필 이미지와 수정 시각을 변경한다")
  void changesProfileImageForActiveMember() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "테스트회원");
    String memberId = member.getId();

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

    LocalDateTime before = LocalDateTime.now(Clock.systemUTC());

    // When
    MemberProfileResult result = service.changeProfileImage(memberId, PROFILE_IMAGE_URL);

    LocalDateTime after = LocalDateTime.now(Clock.systemUTC());

    // Then
    assertThat(result.memberId()).isEqualTo(memberId);
    assertThat(result.profileImage()).isEqualTo(PROFILE_IMAGE_URL);
    assertThat(result.updatedAt()).isBetween(before, after);

    assertThat(member.getProfileImage()).isEqualTo(PROFILE_IMAGE_URL);
    assertThat(member.getUpdatedAt()).isEqualTo(result.updatedAt());

    verify(memberRepository).findById(memberId);
  }

  @Test
  @DisplayName("존재하지 않는 회원의 프로필 이미지 변경을 거부한다")
  void rejectsNonexistentMember() {
    // Given
    String memberId = "unknown-member";

    when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> service.changeProfileImage(memberId, PROFILE_IMAGE_URL))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
  }

  @Test
  @DisplayName("비활성 회원은 프로필 이미지를 변경할 수 없다")
  void rejectsInactiveMember() {
    // Given
    Member member = org.mockito.Mockito.mock(Member.class);
    String memberId = "inactive-member";

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
    when(member.isActive()).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> service.changeProfileImage(memberId, PROFILE_IMAGE_URL))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));

    verify(member, never())
        .changeProfileImage(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class));
  }

  @Test
  @DisplayName("프로필 이미지 약관에 동의한 회원은 검증을 통과한다")
  void acceptsProfileImageConsent() {
    // Given
    String memberId = "member-123";

    when(memberAgreementRepository.existsAgreedActiveAgreement(memberId, "PROFILE_IMAGE"))
        .thenReturn(true);

    // When & Then
    assertThatCode(() -> service.validateProfileImageConsent(memberId)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("프로필 이미지 약관에 동의하지 않은 회원은 등록할 수 없다")
  void rejectsMissingProfileImageConsent() {
    // Given
    String memberId = "member-123";

    when(memberAgreementRepository.existsAgreedActiveAgreement(memberId, "PROFILE_IMAGE"))
        .thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> service.validateProfileImageConsent(memberId))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(MemberErrorCode.PROFILE_IMAGE_CONSENT_REQUIRED));
  }

  @Test
  @DisplayName("닉네임 변경 후 90일이 지나지 않으면 변경을 거부한다")
  void rejectsNicknameChangeBeforeNinetyDays() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "기존닉네임");

    String memberId = member.getId();

    LocalDateTime changedAt = LocalDateTime.now(Clock.systemUTC()).minusDays(30);

    member.changeNickname("기존닉네임", changedAt);

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

    // When & Then
    assertThatThrownBy(() -> service.changeNickname(memberId, "새닉네임"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(MemberErrorCode.NICKNAME_CHANGE_TOO_SOON));

    assertThat(member.getNickname()).isEqualTo("기존닉네임");
    assertThat(member.getNicknameUpdatedAt()).isEqualTo(changedAt);
  }

  @Test
  @DisplayName("회원의 닉네임과 선호 지역 및 테마를 함께 수정한다")
  void updatesMemberProfile() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "기존닉네임");

    String memberId = member.getId();

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
    when(signupService.isNicknameAvailable("새닉네임")).thenReturn(true);
    when(regionQueryService.findRegionIds("11", "110")).thenReturn(List.of(1));

    MemberProfileCommand command =
        new MemberProfileCommand(
            "새닉네임", new PreferredRegion("11", "110"), List.of(Theme.QUIET, Theme.ROMANTIC));

    // When
    MemberProfileDetailsResult result = service.updateProfile(memberId, command);

    // Then
    assertThat(result.memberId()).isEqualTo(memberId);
    assertThat(result.nickname()).isEqualTo("새닉네임");
    assertThat(result.preferredRegionId()).isEqualTo(1);
    assertThat(result.preferredThemes()).containsExactly(Theme.QUIET, Theme.ROMANTIC);
    assertThat(result.updatedAt()).isNotNull();

    assertThat(member.getNickname()).isEqualTo("새닉네임");
    assertThat(member.getPreferredRegionId()).isEqualTo(1);
    assertThat(member.getPreferredThemes()).containsExactly(Theme.QUIET, Theme.ROMANTIC);
  }

  @Test
  @DisplayName("선호 지역과 테마 선택을 해제한다")
  void clearsMemberPreferences() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "기존닉네임");

    LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

    member.changePreferredRegion(1, now);
    member.changePreferredThemes(List.of(Theme.QUIET), now);

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

    MemberProfileCommand command =
        new MemberProfileCommand(null, new PreferredRegion(null, null), List.of());

    // When
    service.updateProfile(member.getId(), command);

    // Then
    assertThat(member.getPreferredRegionId()).isNull();
    assertThat(member.getPreferredThemes()).isEmpty();
  }

  @Test
  @DisplayName("선호 테마를 중복 선택하면 변경을 거부한다")
  void rejectsDuplicateThemes() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "기존닉네임");

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

    MemberProfileCommand command =
        new MemberProfileCommand(null, null, List.of(Theme.QUIET, Theme.QUIET));

    // When & Then
    assertThatThrownBy(() -> service.updateProfile(member.getId(), command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("선호 테마를 중복 선택할 수 없습니다.");

    assertThat(member.getPreferredThemes()).isEmpty();
  }

  @Test
  @DisplayName("수정할 회원정보가 없으면 요청을 거부한다")
  void rejectsEmptyProfileUpdate() {
    MemberProfileCommand command = new MemberProfileCommand(null, null, null);

    assertThatThrownBy(() -> service.updateProfile("member-1", command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("수정할 회원정보가 없습니다.");
  }

  @Test
  @DisplayName("닉네임만 수정하면 기존 선호 지역과 테마를 유지한다")
  void keepsPreferencesWhenOnlyNicknameChanges() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "기존닉네임");

    LocalDateTime previousUpdatedAt = LocalDateTime.now(Clock.systemUTC()).minusDays(1);

    member.changePreferredRegion(17, previousUpdatedAt);
    member.changePreferredThemes(List.of(Theme.QUIET), previousUpdatedAt);

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
    when(signupService.isNicknameAvailable("새닉네임")).thenReturn(true);

    MemberProfileCommand command = new MemberProfileCommand("새닉네임", null, null);

    // When
    MemberProfileDetailsResult result = service.updateProfile(member.getId(), command);

    // Then
    assertThat(result.nickname()).isEqualTo("새닉네임");
    assertThat(result.preferredRegionId()).isEqualTo(17);
    assertThat(result.preferredThemes()).containsExactly(Theme.QUIET);

    assertThat(member.getPreferredRegionId()).isEqualTo(17);
    assertThat(member.getPreferredThemes()).containsExactly(Theme.QUIET);

    verifyNoInteractions(regionQueryService);
  }

  @Test
  @DisplayName("선호 테마만 수정하면 닉네임과 선호 지역을 유지한다")
  void keepsOtherFieldsWhenOnlyThemesChange() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "기존닉네임");

    LocalDateTime previousUpdatedAt = LocalDateTime.now(Clock.systemUTC()).minusDays(1);

    member.changePreferredRegion(17, previousUpdatedAt);
    member.changePreferredThemes(List.of(Theme.QUIET), previousUpdatedAt);

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

    MemberProfileCommand command =
        new MemberProfileCommand(null, null, List.of(Theme.ROMANTIC, Theme.COZY));

    // When
    MemberProfileDetailsResult result = service.updateProfile(member.getId(), command);

    // Then
    assertThat(result.nickname()).isEqualTo("기존닉네임");
    assertThat(result.preferredRegionId()).isEqualTo(17);
    assertThat(result.preferredThemes()).containsExactly(Theme.ROMANTIC, Theme.COZY);

    assertThat(member.getNickname()).isEqualTo("기존닉네임");
    assertThat(member.getNicknameUpdatedAt()).isNull();

    verifyNoInteractions(signupService, regionQueryService);
  }
}
