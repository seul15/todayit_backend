package com.todayit.member.service;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.command.MemberProfileCommand;
import com.todayit.member.service.model.MemberProfileDetailsResult;
import com.todayit.member.service.model.MemberProfileResult;
import com.todayit.theme.Theme;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 프로필 변경과 약관 동의 검증을 처리합니다. */
@Service
public class MemberProfileService {

  private static final String PROFILE_IMAGE_AGREEMENT_CODE = "PROFILE_IMAGE";

  private final MemberRepository memberRepository;
  private final MemberAgreementRepository memberAgreementRepository;
  private final SignupService signupService;

  /**
   * 회원 프로필 변경에 필요한 서비스를 주입받습니다.
   *
   * @param memberRepository 회원 Repository
   * @param memberAgreementRepository 회원 약관 동의 Repository
   * @param signupService 공통 닉네임 검증 서비스
   */
  public MemberProfileService(
      MemberRepository memberRepository,
      MemberAgreementRepository memberAgreementRepository,
      SignupService signupService) {
    this.memberRepository = memberRepository;
    this.memberAgreementRepository = memberAgreementRepository;
    this.signupService = signupService;
  }

  /**
   * 회원의 프로필 이미지 URL과 수정 시각을 변경합니다.
   *
   * @param memberId 변경할 회원 식별자
   * @param profileImage 업로드가 완료된 프로필 이미지 URL
   * @return 변경된 회원 프로필 이미지 정보
   * @throws BusinessException 회원이 없거나 비활성 상태인 경우
   */
  @Transactional
  public MemberProfileResult changeProfileImage(String memberId, String profileImage) {
    Member member = findActiveMember(memberId);

    LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
    member.changeProfileImage(profileImage, now);

    return new MemberProfileResult(member.getId(), member.getProfileImage(), member.getUpdatedAt());
  }

  /**
   * 회원 닉네임을 변경합니다.
   *
   * @param memberId 변경할 회원 식별자
   * @param nickname 변경할 닉네임
   * @throws BusinessException 회원이 없거나 닉네임이 중복되었거나 변경 주기를 위반한 경우
   * @throws InvalidRequestException 닉네임 형식이 올바르지 않은 경우
   */
  @Transactional
  public void changeNickname(String memberId, String nickname) {
    Member member = findActiveMember(memberId);
    LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

    String normalizedNickname = validateNicknameChange(member, nickname, now);

    if (normalizedNickname != null) {
      member.changeNickname(normalizedNickname, now);
    }
  }

  /**
   * 회원의 닉네임, 선호 지역, 선호 테마를 수정합니다.
   *
   * <p>요청에서 생략한 필드는 유지하며, 모든 값을 검증한 후 변경합니다.
   *
   * @param memberId 변경할 회원 식별자
   * @param command 회원 프로필 변경 정보
   * @return 변경된 회원 프로필 정보
   * @throws BusinessException 회원이 없거나 닉네임 변경 정책을 위반한 경우
   * @throws InvalidRequestException 수정 항목이 없거나 입력값이 잘못된 경우
   */
  @Transactional
  public MemberProfileDetailsResult updateProfile(String memberId, MemberProfileCommand command) {

    if (command == null
        || (command.nickname() == null
            && command.preferredRegions() == null
            && command.preferredThemes() == null)) {
      throw new InvalidRequestException("수정할 회원정보가 없습니다.");
    }

    Member member = findActiveMember(memberId);
    LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

    // 닉네임 검증
    String normalizedNickname =
        command.nickname() == null ? null : validateNicknameChange(member, command.nickname(), now);

    // 선호 지역 검증
    List<String> regions = command.preferredRegions();

    if (regions != null) {
      if (regions.size() > 3) {
        throw new InvalidRequestException("선호 지역은 최대 3개까지 선택할 수 있습니다.");
      }

      if (regions.stream().anyMatch(code -> code == null || code.isBlank())) {
        throw new InvalidRequestException("선호 지역 목록이 올바르지 않습니다.");
      }

      if (new HashSet<>(regions).size() != regions.size()) {
        throw new InvalidRequestException("선호 지역을 중복 선택할 수 없습니다.");
      }
    }

    // 선호 테마 검증
    List<Theme> themes = command.preferredThemes();

    if (themes != null && new HashSet<>(themes).size() != themes.size()) {
      throw new InvalidRequestException("선호 테마를 중복 선택할 수 없습니다.");
    }

    // 검증 완료 후 닉네임 변경
    if (normalizedNickname != null) {
      member.changeNickname(normalizedNickname, now);
    }

    // 선호 지역 변경
    if (regions != null && !member.getPreferredRegions().equals(regions)) {
      member.changePreferredRegions(regions, now);
    }

    // 선호 테마 변경
    if (themes != null && !member.getPreferredThemes().equals(themes)) {
      member.changePreferredThemes(themes, now);
    }

    return toDetailsResult(member);
  }

  /**
   * 로그인한 회원 본인의 프로필 정보를 조회합니다.
   *
   * @param memberId 인증된 회원 식별자
   * @return 회원 프로필 조회 결과
   * @throws BusinessException 회원이 없거나 비활성 상태인 경우
   */
  @Transactional(readOnly = true)
  public MemberProfileDetailsResult findProfile(String memberId) {
    Member member = findActiveMember(memberId);
    return toDetailsResult(member);
  }

  /**
   * 프로필 이미지 등록을 위한 약관 동의 여부를 확인합니다.
   *
   * @param memberId 회원 식별자
   * @throws BusinessException 약관에 동의하지 않은 경우
   */
  @Transactional(readOnly = true)
  public void validateProfileImageConsent(String memberId) {
    boolean agreed =
        memberAgreementRepository.existsAgreedActiveAgreement(
            memberId, PROFILE_IMAGE_AGREEMENT_CODE);

    if (!agreed) {
      throw new BusinessException(MemberErrorCode.PROFILE_IMAGE_CONSENT_REQUIRED);
    }
  }

  private Member findActiveMember(String memberId) {
    return memberRepository
        .findById(memberId)
        .filter(Member::isActive)
        .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
  }

  private String validateNicknameChange(Member member, String nickname, LocalDateTime now) {

    if (nickname == null || nickname.isBlank()) {
      throw new InvalidRequestException("닉네임은 필수입니다.");
    }

    String normalizedNickname = nickname.strip();

    // 동일한 닉네임이면 변경할 필요 없음
    if (member.getNickname().equalsIgnoreCase(normalizedNickname)) {
      return null;
    }

    LocalDateTime lastUpdatedAt = member.getNicknameUpdatedAt();

    // 최초 변경은 허용, 이후 변경은 90일 제한
    if (lastUpdatedAt != null && now.isBefore(lastUpdatedAt.plusDays(90))) {
      throw new BusinessException(MemberErrorCode.NICKNAME_CHANGE_TOO_SOON);
    }

    // 기존 회원가입과 동일한 형식·금칙어·중복 검증
    if (!signupService.isNicknameAvailable(normalizedNickname)) {
      throw new BusinessException(MemberErrorCode.NICKNAME_ALREADY_EXISTS);
    }

    return normalizedNickname;
  }

  private MemberProfileDetailsResult toDetailsResult(Member member) {
    return new MemberProfileDetailsResult(
        member.getId(),
        member.getEmail(),
        member.getNickname(),
        member.getRegisteredProfileImage(),
        member.getPreferredRegions(),
        member.getPreferredThemes(),
        member.getUpdatedAt());
  }

  /**
   * 회원의 프로필 이미지를 기본 이미지로 복원합니다.
   *
   * @param memberId 회원 식별자
   * @return 초기화된 프로필 이미지 정보
   * @throws BusinessException 회원이 없거나 비활성 상태인 경우
   */
  @Transactional
  public MemberProfileResult resetProfileImage(String memberId) {
    Member member = findActiveMember(memberId);

    // 이미 기본 이미지라면 수정 시각을 변경하지 않음
    if (member.getRegisteredProfileImage() == null) {
      return new MemberProfileResult(member.getId(), null, member.getUpdatedAt());
    }

    LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
    member.resetProfileImage(now);

    return new MemberProfileResult(
        member.getId(), member.getRegisteredProfileImage(), member.getUpdatedAt());
  }
}
