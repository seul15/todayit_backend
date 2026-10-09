package com.todayit.member.service;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.BlocklistMatchType;
import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberAgreement;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.entity.MemberRole;
import com.todayit.member.exception.InvalidEmailVerificationTokenException;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.repository.MemberRoleRepository;
import com.todayit.member.repository.NicknameBlocklistRepository;
import com.todayit.member.repository.PasswordBlocklistRepository;
import com.todayit.member.service.command.SignupCommand;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 회원가입 관련 기능을 처리합니다. */
@Service
public class SignupService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;
  private final MemberRoleRepository memberRoleRepository;
  private final MemberAgreementRepository memberAgreementRepository;
  private final PasswordBlocklistRepository passwordBlocklistRepository;
  private final NicknameBlocklistRepository nicknameBlocklistRepository;

  private static final String DEFAULT_ROLE_NAME = "USER";

  /**
   * 회원가입에 필요한 회원 저장소를 받습니다.
   *
   * @param memberRepository 회원 Repository
   * @param passwordEncoder 비밀번호 암호화
   * @param emailVerificationService 이메일 인증 Service
   * @param memberRoleRepository 회원 권한 Repository
   * @param memberAgreementRepository 회원 약관 동의 Repository
   * @param passwordBlocklistRepository 비밀번호 금칙어 Repository
   * @param nicknameBlocklistRepository 닉네임 금칙어 Repository
   */
  public SignupService(
      MemberRepository memberRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationService emailVerificationService,
      MemberRoleRepository memberRoleRepository,
      MemberAgreementRepository memberAgreementRepository,
      PasswordBlocklistRepository passwordBlocklistRepository,
      NicknameBlocklistRepository nicknameBlocklistRepository) {
    this.memberRepository = memberRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
    this.memberRoleRepository = memberRoleRepository;
    this.memberAgreementRepository = memberAgreementRepository;
    this.passwordBlocklistRepository = passwordBlocklistRepository;
    this.nicknameBlocklistRepository = nicknameBlocklistRepository;
  }

  /**
   * 로컬 회원가입에 사용할 수 있는 이메일인지 확인합니다.
   *
   * @param email 확인할 이메일
   * @return 사용 가능하면 true
   */
  @Transactional(readOnly = true)
  public boolean isEmailAvailable(String email) {
    return !memberRepository.existsByEmailAndProvider(email, MemberProvider.LOCAL);
  }

  /**
   * 이메일 인증을 확인한 뒤 로컬 회원을 생성하고 기본 권한과 약관 동의 정보를 저장합니다.
   *
   * @param command 회원가입에 필요한 정보
   * @return 저장된 회원
   * @throws BusinessException 이미 사용 중인 로컬 이메일인 경우
   * @throws InvalidRequestException 필수 약관에 동의하지 않은 경우
   * @throws InvalidEmailVerificationTokenException 이메일 인증 정보가 유효하지 않은 경우
   * @throws IllegalStateException 필수 약관 또는 기본 USER 권한 정보가 존재하지 않는 경우
   */
  @Transactional
  public Member createLocalMember(SignupCommand command) {
    // 이미 가입된 LOCAL 이메일인지 확인
    if (memberRepository.existsByEmailAndProvider(command.email(), MemberProvider.LOCAL)) {
      throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
    }

    // 현재 적용 중인 필수 약관 조회

    // 현재 적용 중인 약관 조회
    List<MemberAgreementRepository.CurrentAgreement> currentAgreements =
        memberAgreementRepository.findCurrentAgreements();

    // 현재 적용 중인 필수 약관이 없으면 회원가입을 진행하지 않음
    boolean hasRequiredAgreement =
        currentAgreements.stream()
            .anyMatch(agreement -> Boolean.TRUE.equals(agreement.getRequired()));

    if (!hasRequiredAgreement) {
      throw new IllegalStateException("필수 약관 정보가 없습니다.");
    }

    // 약관 코드를 실제 DB 약관 정보와 연결
    Map<String, MemberAgreementRepository.CurrentAgreement> agreementsByCode =
        currentAgreements.stream()
            .collect(
                Collectors.toMap(
                    MemberAgreementRepository.CurrentAgreement::getCode, agreement -> agreement));

    // 존재하지 않거나 현재 적용되지 않는 약관 코드는 거부
    boolean hasUnknownAgreement =
        command.agreements().keySet().stream()
            .anyMatch(code -> !agreementsByCode.containsKey(code));

    if (hasUnknownAgreement) {
      throw new InvalidRequestException("유효하지 않은 약관 코드가 포함되어 있습니다.");
    }

    // 동의 여부는 반드시 true 또는 false
    if (command.agreements().values().stream().anyMatch(agreed -> agreed == null)) {
      throw new InvalidRequestException("약관 동의 여부를 확인해 주세요.");
    }

    // DB에서 필수로 설정한 약관은 반드시 동의해야 함
    boolean hasMissingRequiredAgreement =
        currentAgreements.stream()
            .filter(agreement -> Boolean.TRUE.equals(agreement.getRequired()))
            .anyMatch(
                agreement -> !Boolean.TRUE.equals(command.agreements().get(agreement.getCode())));

    if (hasMissingRequiredAgreement) {
      throw new InvalidRequestException("필수 약관과 동의 항목을 확인해 주세요.");
    }

    emailVerificationService.validateVerificationToken(
        command.email(), EmailVerificationPurpose.SIGNUP, command.emailVerificationToken());

    if (passwordBlocklistRepository.existsByWordAndMatchTypeAndEnabledTrue(
        command.password(), BlocklistMatchType.EXACT)) {
      throw new InvalidRequestException("쉽게 추측되거나 유출된 비밀번호입니다. 다른 비밀번호를 입력해 주세요.");
    }

    if (nicknameBlocklistRepository.existsBlockedNickname(command.nickname())) {
      throw new InvalidRequestException("사용할 수 없는 닉네임입니다.");
    }

    String encodedPassword = passwordEncoder.encode(command.password());

    Member member = Member.createLocal(command.email(), encodedPassword, command.nickname());

    // 생성된 회원 정보를 DB에 저장
    Member savedMember = memberRepository.save(member);

    // 신규 회원에게 부여할 기본 USER 권한 식별자 조회
    Integer userRoleId =
        memberRoleRepository
            .findRoleIdByName(DEFAULT_ROLE_NAME)
            .orElseThrow(() -> new IllegalStateException("USER 권한 정보가 없습니다."));

    // 회원과 USER 권한 연결 정보 저장
    memberRoleRepository.save(MemberRole.create(savedMember.getId(), userRoleId));

    // 약관 코드를 실제 agreement_id로 변환해서 동의 정보 저장
    command
        .agreements()
        .forEach(
            (code, agreed) -> {
              Integer agreementId = agreementsByCode.get(code).getAgreementId();

              memberAgreementRepository.save(
                  MemberAgreement.create(agreementId, savedMember.getId(), agreed));
            });

    // 회원가입에 사용한 이메일 인증 토큰 재사용 방지를 위해 삭제
    emailVerificationService.invalidateVerificationToken(command.emailVerificationToken());

    return savedMember;
  }
}
