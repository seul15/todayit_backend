package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.course.service.RegionPlaceQueryService;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.PreferredRegionCode;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.command.MemberProfileCommand;
import com.todayit.member.service.model.MemberProfileDetailsResult;
import com.todayit.member.service.model.MemberProfileResult;
import com.todayit.theme.Theme;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** 회원 프로필 이미지 변경과 회원 상태 검증을 테스트합니다. */
@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

  private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";

  @Mock private MemberRepository memberRepository;
  @Mock private MemberAgreementRepository memberAgreementRepository;
  @Mock private SignupService signupService;
  @Mock private RegionPlaceQueryService regionPlaceQueryService;

  private MemberProfileService service;

  @BeforeEach
  void setUp() {
    service =
        new MemberProfileService(
            memberRepository, memberAgreementRepository, signupService, regionPlaceQueryService);
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

    MemberProfileCommand command =
        new MemberProfileCommand(
            "새닉네임",
            List.of(new PreferredRegionCode("11", "110")),
            List.of(Theme.QUIET, Theme.ROMANTIC));

    // When
    MemberProfileDetailsResult result = service.updateProfile(memberId, command);

    // Then
    assertThat(result.preferredRegions()).containsExactly(new PreferredRegionCode("11", "110"));

    assertThat(member.getPreferredSidoCode()).isEqualTo("11");
    assertThat(member.getPreferredSigunguCode()).isEqualTo("110");

    verify(regionPlaceQueryService).findRegionIds("11", "110");
  }

  @Test
  @DisplayName("선호 지역과 테마 선택을 해제한다")
  void clearsMemberPreferences() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "기존닉네임");

    LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

    member.changePreferredRegion("11", "110", now);
    member.changePreferredThemes(List.of(Theme.QUIET), now);

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

    MemberProfileCommand command = new MemberProfileCommand(null, List.of(), List.of());

    // When
    service.updateProfile(member.getId(), command);

    // Then
    assertThat(member.getPreferredSidoCode()).isNull();
    assertThat(member.getPreferredSigunguCode()).isNull();
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
  @DisplayName("프로필 이미지를 삭제하면 기본 이미지로 복원한다")
  void resetsProfileImage() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "테스트회원");

    member.changeProfileImage(PROFILE_IMAGE_URL, LocalDateTime.now(Clock.systemUTC()).minusDays(1));

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

    // When
    MemberProfileResult result = service.resetProfileImage(member.getId());

    // Then
    assertThat(result.profileImage()).isNull();
    assertThat(member.getRegisteredProfileImage()).isNull();
    assertThat(result.updatedAt()).isNotNull();
  }

  @Test
  @DisplayName("이미 기본 이미지인 경우 수정 시각을 변경하지 않는다")
  void keepsUpdatedAtWhenProfileImageAlreadyDefault() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "테스트회원");

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

    LocalDateTime previousUpdatedAt = member.getUpdatedAt();

    // When
    MemberProfileResult result = service.resetProfileImage(member.getId());

    // Then
    assertThat(result.profileImage()).isNull();
    assertThat(result.updatedAt()).isEqualTo(previousUpdatedAt);
  }

  @Test
  @DisplayName("닉네임 저장 시 DB 중복 오류가 발생하면 409 오류 코드로 변환한다")
  void convertsNicknameUniqueViolation() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "기존닉네임");

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

    when(signupService.isNicknameAvailable("새닉네임")).thenReturn(true);

    ConstraintViolationException constraint = mock(ConstraintViolationException.class);

    when(constraint.getConstraintName()).thenReturn("uk_member_nickname_normalized");

    doThrow(new DataIntegrityViolationException("닉네임 중복", constraint))
        .when(memberRepository)
        .flush();

    MemberProfileCommand command = new MemberProfileCommand("새닉네임", null, null);

    // When & Then
    assertThatThrownBy(() -> service.updateProfile(member.getId(), command))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(MemberErrorCode.NICKNAME_ALREADY_EXISTS));
  }

  @Test
  @DisplayName("허용 목록에 없는 선호 지역을 거부한다")
  void rejectsUnsupportedPreferredRegion() {
    // Given
    Member member = Member.createLocal("test@example.com", "encoded-password", "테스트회원");

    when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

    MemberProfileCommand command =
        new MemberProfileCommand(null, List.of(new PreferredRegionCode("11", "999")), null);

    // When & Then
    assertThatThrownBy(() -> service.updateProfile(member.getId(), command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("선택할 수 없는 선호 지역입니다.");

    assertThat(member.getPreferredSidoCode()).isNull();
    assertThat(member.getPreferredSigunguCode()).isNull();
  }
}
