package com.todayit.member.service;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.course.entity.Region;
import com.todayit.course.exception.InvalidRegionPathException;
import com.todayit.course.repository.RegionRepository;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.PreferredRegionCode;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.command.MemberProfileCommand;
import com.todayit.member.service.model.MemberProfileDetailsResult;
import com.todayit.member.service.model.MemberProfileResult;
import com.todayit.theme.Theme;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 프로필 변경과 약관 동의 검증을 처리합니다. */
@Service
public class MemberProfileService {

  private static final String PROFILE_IMAGE_AGREEMENT_CODE = "PROFILE_IMAGE";
  private static final String PREFERENCE_AGREEMENT_CODE = "PREFERENCE";

  private static final Set<PreferredRegionCode> ALLOWED_PREFERRED_REGIONS =
      Set.of(
          // 서울특별시
          new PreferredRegionCode("11", "440"),
          new PreferredRegionCode("11", "200"),
          new PreferredRegionCode("11", "680"),
          new PreferredRegionCode("11", "710"),
          new PreferredRegionCode("11", "110"),
          new PreferredRegionCode("11", "170"),
          new PreferredRegionCode("11", "560"),
          new PreferredRegionCode("11", "140"),
          new PreferredRegionCode("11", "650"),
          new PreferredRegionCode("11", "215"),

          // 경기도
          new PreferredRegionCode("41", "115"),
          new PreferredRegionCode("41", "135"),
          new PreferredRegionCode("41", "285"),
          new PreferredRegionCode("41", "480"),
          new PreferredRegionCode("41", "820"),

          // 인천광역시
          new PreferredRegionCode("28", "185"),

          // 부산광역시
          new PreferredRegionCode("26", "500"),
          new PreferredRegionCode("26", "350"),
          new PreferredRegionCode("26", "230"),

          // 강원특별자치도
          new PreferredRegionCode("51", "150"),
          new PreferredRegionCode("51", "210"),
          new PreferredRegionCode("51", "110"),

          // 제주특별자치도
          new PreferredRegionCode("50", "110"),
          new PreferredRegionCode("50", "130"),

          // 기타 지역
          new PreferredRegionCode("47", "130"),
          new PreferredRegionCode("52", "111"),
          new PreferredRegionCode("12", "130"),
          new PreferredRegionCode("30", "140"),
          new PreferredRegionCode("27", "110"),
          new PreferredRegionCode("44", "131"));

  private final MemberRepository memberRepository;
  private final MemberAgreementRepository memberAgreementRepository;
  private final SignupService signupService;
  private final RegionRepository regionRepository;

  /**
   * 회원 프로필 변경에 필요한 서비스를 주입받습니다.
   *
   * @param memberRepository 회원 Repository
   * @param memberAgreementRepository 회원 약관 동의 Repository
   * @param signupService 닉네임 검증 서비스
   * @param regionRepository 기존 지역 Repository
   */
  public MemberProfileService(
      MemberRepository memberRepository,
      MemberAgreementRepository memberAgreementRepository,
      SignupService signupService,
      RegionRepository regionRepository) {
    this.memberRepository = memberRepository;
    this.memberAgreementRepository = memberAgreementRepository;
    this.signupService = signupService;
    this.regionRepository = regionRepository;
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
    Member member = findActiveMemberForUpdate(memberId);
    LocalDateTime now = LocalDateTime.now();
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
    Member member = findActiveMemberForUpdate(memberId);
    LocalDateTime now = LocalDateTime.now();

    String normalizedNickname = validateNicknameChange(member, nickname, now);

    if (normalizedNickname != null) {
      member.changeNickname(normalizedNickname, now);
      flushNicknameChange();
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
   * @throws InvalidRegionPathException 선택할 수 없거나 존재하지 않는 지역인 경우
   */
  @Transactional
  public MemberProfileDetailsResult updateProfile(String memberId, MemberProfileCommand command) {

    if (command == null
        || (command.nickname() == null
            && command.preferredRegions() == null
            && command.preferredThemes() == null)) {
      throw new InvalidRequestException("수정할 회원정보가 없습니다.");
    }

    Member member = findActiveMemberForUpdate(memberId);
    LocalDateTime now = LocalDateTime.now();

    // 닉네임 검증
    String normalizedNickname =
        command.nickname() == null ? null : validateNicknameChange(member, command.nickname(), now);

    // 선호 지역 검증
    List<PreferredRegionCode> regions = command.preferredRegions();
    Region selectedRegion = null;

    if (regions != null) {
      if (regions.size() > 1) {
        throw new InvalidRequestException("선호 지역은 1개만 선택할 수 있습니다.");
      }

      if (!regions.isEmpty()) {
        PreferredRegionCode requested = regions.get(0);

        if (requested == null
            || requested.sidoCode() == null
            || requested.sigunguCode() == null
            || !requested.sidoCode().matches("[0-9]{2}")
            || !requested.sigunguCode().matches("[0-9]{3}")) {
          throw new InvalidRequestException("선호 지역 코드가 올바르지 않습니다.");
        }

        if (!ALLOWED_PREFERRED_REGIONS.contains(requested)) {
          throw new InvalidRegionPathException();
        }

        // 기존 Repository 메서드 재사용
        List<Integer> regionIds =
            regionRepository.findIdsByCodes(requested.sidoCode(), requested.sigunguCode());

        if (regionIds.size() != 1) {
          throw new InvalidRegionPathException();
        }

        // JpaRepository 기본 제공 메서드 재사용
        selectedRegion =
            regionRepository
                .findById(regionIds.get(0))
                .orElseThrow(InvalidRegionPathException::new);
      }
    }

    // 선호 테마 검증
    List<Theme> themes = command.preferredThemes();

    if (themes != null && new HashSet<>(themes).size() != themes.size()) {
      throw new InvalidRequestException("선호 테마를 중복 선택할 수 없습니다.");
    }

    // 선호 지역 또는 테마를 저장하는 경우 약관 동의 확인
    boolean settingPreference =
        (regions != null && !regions.isEmpty()) || (themes != null && !themes.isEmpty());

    if (settingPreference
        && !memberAgreementRepository.existsAgreedActiveAgreement(
            memberId, PREFERENCE_AGREEMENT_CODE)) {
      throw new BusinessException(MemberErrorCode.PREFERENCE_CONSENT_REQUIRED);
    }

    // 검증 완료 후 닉네임 변경
    if (normalizedNickname != null) {
      member.changeNickname(normalizedNickname, now);
      flushNicknameChange();
    }

    // 선호 지역 변경
    if (regions != null) {
      Region currentRegion = member.getPreferredRegion();

      Integer currentId = currentRegion == null ? null : currentRegion.getRegionId();

      Integer selectedId = selectedRegion == null ? null : selectedRegion.getRegionId();

      if (!Objects.equals(currentId, selectedId)) {
        member.changePreferredRegion(selectedRegion, now);
      }
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

  private Member findActiveMemberForUpdate(String memberId) {
    return memberRepository
        .findByIdForUpdate(memberId)
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
    Region region = member.getPreferredRegion();

    List<PreferredRegionCode> regions =
        region == null
            ? List.of()
            : List.of(new PreferredRegionCode(region.getSidoCode(), region.getSigunguCode()));

    return new MemberProfileDetailsResult(
        member.getId(),
        member.getEmail(),
        member.getNickname(),
        member.getRegisteredProfileImage(),
        regions,
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
    Member member = findActiveMemberForUpdate(memberId);

    // 이미 기본 이미지라면 수정 시각을 변경하지 않음
    if (member.getRegisteredProfileImage() == null) {
      return new MemberProfileResult(member.getId(), null, member.getUpdatedAt());
    }

    LocalDateTime now = LocalDateTime.now();
    member.resetProfileImage(now);

    return new MemberProfileResult(
        member.getId(), member.getRegisteredProfileImage(), member.getUpdatedAt());
  }

  private void flushNicknameChange() {
    try {
      // 트랜잭션 종료 전 DB UNIQUE 제약 검증
      memberRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      Throwable cause = exception;

      while (cause != null) {
        if (cause instanceof ConstraintViolationException constraint
            && "uk_member_nickname_normalized".equalsIgnoreCase(constraint.getConstraintName())) {
          throw new BusinessException(MemberErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        cause = cause.getCause();
      }

      // 닉네임 중복 이외의 DB 오류는 그대로 전파
      throw exception;
    }
  }
}
