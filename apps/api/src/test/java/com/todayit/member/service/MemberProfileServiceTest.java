package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.BusinessException;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.model.MemberProfileResult;
import java.time.Clock;
import java.time.LocalDateTime;
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

  private MemberProfileService service;

  @BeforeEach
  void setUp() {
    service = new MemberProfileService(memberRepository, memberAgreementRepository, signupService);
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
}
