package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.BlocklistMatchType;
import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberAgreement;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.entity.MemberRole;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.repository.MemberRoleRepository;
import com.todayit.member.repository.NicknameBlocklistRepository;
import com.todayit.member.repository.PasswordBlocklistRepository;
import com.todayit.member.service.command.SignupCommand;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

  @Mock private MemberRepository memberRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailVerificationService emailVerificationService;
  @Mock private MemberRoleRepository memberRoleRepository;
  @Mock private MemberAgreementRepository memberAgreementRepository;
  @Mock private PasswordBlocklistRepository passwordBlocklistRepository;
  @Mock private NicknameBlocklistRepository nicknameBlocklistRepository;

  private SignupService signupService;

  private static final List<MemberAgreementRepository.CurrentAgreement> CURRENT_AGREEMENTS =
      List.of(
          new TestAgreement(10, "TOS", true),
          new TestAgreement(20, "ACCOUNT_PRIVACY", false),
          new TestAgreement(30, "PREFERENCE", false),
          new TestAgreement(40, "PROFILE_IMAGE", false));

  @BeforeEach
  void setUp() {
    signupService =
        new SignupService(
            memberRepository,
            passwordEncoder,
            emailVerificationService,
            memberRoleRepository,
            memberAgreementRepository,
            passwordBlocklistRepository,
            nicknameBlocklistRepository);
  }

  @Test
  @DisplayName("회원가입 시 약관 코드를 실제 agreement_id로 변환하여 저장한다")
  void createsLocalMemberWithAgreementCodes() {
    String email = "test@test.com";
    String password = "password123!";
    String verificationToken = "verification-token";

    Map<String, Boolean> agreements =
        Map.of(
            "TOS", true,
            "ACCOUNT_PRIVACY", true,
            "PREFERENCE", false,
            "PROFILE_IMAGE", false);

    SignupCommand command =
        new SignupCommand(email, verificationToken, password, "테스트", agreements);

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);
    when(passwordEncoder.encode(password)).thenReturn("encoded-password");
    when(memberRepository.save(any(Member.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(memberRoleRepository.findRoleIdByName("USER")).thenReturn(Optional.of(1));

    Member savedMember = signupService.createLocalMember(command);

    assertThat(savedMember.getPassword()).isEqualTo("encoded-password");
    assertThat(savedMember.getCreatedAt()).isNotNull();

    verify(emailVerificationService)
        .validateVerificationToken(email, EmailVerificationPurpose.SIGNUP, verificationToken);
    verify(emailVerificationService).invalidateVerificationToken(verificationToken);
    verify(memberRoleRepository).save(any(MemberRole.class));

    ArgumentCaptor<MemberAgreement> captor = ArgumentCaptor.forClass(MemberAgreement.class);

    verify(memberAgreementRepository, times(4)).save(captor.capture());

    List<Integer> savedAgreementIds =
        captor.getAllValues().stream()
            .map(agreement -> (Integer) ReflectionTestUtils.getField(agreement, "agreementId"))
            .toList();

    assertThat(savedAgreementIds).containsExactlyInAnyOrder(10, 20, 30, 40);
  }

  @Test
  @DisplayName("이미 가입된 이메일이면 회원가입을 거부한다")
  void rejectsSignupWhenLocalEmailAlreadyExists() {
    SignupCommand command =
        new SignupCommand(
            "test@test.com", "verification-token", "password123!", "테스트", Map.of("TOS", true));

    when(memberRepository.existsByEmailAndProvider("test@test.com", MemberProvider.LOCAL))
        .thenReturn(true);

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(BusinessException.class)
        .hasMessage("이미 사용 중인 이메일입니다.");

    verify(emailVerificationService, never()).validateVerificationToken(any(), any(), any());
    verify(memberRepository, never()).save(any());
  }

  @Test
  @DisplayName("필수 약관에 동의하지 않으면 회원가입을 거부한다")
  void rejectsSignupWhenRequiredAgreementIsNotAccepted() {
    SignupCommand command =
        new SignupCommand(
            "test@test.com",
            "verification-token",
            "password123!",
            "테스트",
            Map.of(
                "TOS", false,
                "ACCOUNT_PRIVACY", true,
                "PREFERENCE", false,
                "PROFILE_IMAGE", false));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("필수 약관과 동의 항목을 확인해 주세요.");

    verify(emailVerificationService, never()).validateVerificationToken(any(), any(), any());
    verify(memberRepository, never()).save(any());
  }

  @Test
  @DisplayName("필수 약관 코드가 누락되면 회원가입을 거부한다")
  void rejectsSignupWhenRequiredAgreementIsMissing() {
    SignupCommand command =
        new SignupCommand(
            "test@test.com",
            "verification-token",
            "password123!",
            "테스트",
            Map.of("PREFERENCE", false));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("필수 약관과 동의 항목을 확인해 주세요.");

    verify(memberRepository, never()).save(any());
  }

  @Test
  @DisplayName("필수 약관 정보가 없으면 회원가입을 진행하지 않는다")
  void rejectsSignupWhenRequiredAgreementInformationDoesNotExist() {
    SignupCommand command =
        new SignupCommand(
            "test@test.com", "verification-token", "password123!", "테스트", Map.of("TOS", true));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(List.of());

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("필수 약관 정보가 없습니다.");

    verify(memberRepository, never()).save(any());
  }

  @Test
  @DisplayName("존재하지 않는 약관 코드를 전달하면 회원가입을 거부한다")
  void rejectsSignupWhenAgreementCodeIsUnknown() {
    SignupCommand command =
        new SignupCommand(
            "test@test.com",
            "verification-token",
            "password123!",
            "테스트",
            Map.of(
                "TOS", true,
                "marketing", false));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("유효하지 않은 약관 코드가 포함되어 있습니다.");

    verify(memberRepository, never()).save(any());
  }

  @Test
  @DisplayName("선택 약관을 전달하지 않아도 회원가입할 수 있다")
  void allowsSignupWithoutOptionalAgreements() {
    SignupCommand command =
        new SignupCommand(
            "test@test.com", "verification-token", "password123!", "테스트", Map.of("TOS", true));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);
    when(passwordEncoder.encode("password123!")).thenReturn("encoded-password");
    when(memberRepository.save(any(Member.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(memberRoleRepository.findRoleIdByName("USER")).thenReturn(Optional.of(1));

    Member savedMember = signupService.createLocalMember(command);

    assertThat(savedMember).isNotNull();
    verify(memberAgreementRepository, times(1)).save(any(MemberAgreement.class));
  }

  @Test
  @DisplayName("금칙어 비밀번호이면 회원가입을 거부한다")
  void rejectsSignupWhenPasswordIsBlocked() {
    String password = "password123!";

    SignupCommand command =
        new SignupCommand(
            "test@test.com", "verification-token", password, "테스트", Map.of("TOS", true));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);
    when(passwordBlocklistRepository.existsByWordAndMatchTypeAndEnabledTrue(
            password, BlocklistMatchType.EXACT))
        .thenReturn(true);

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("쉽게 추측되거나 유출된 비밀번호입니다. 다른 비밀번호를 입력해 주세요.");

    verify(passwordEncoder, never()).encode(any());
    verify(memberRepository, never()).save(any());
  }

  @Test
  @DisplayName("금칙어 닉네임이면 회원가입을 거부한다")
  void rejectsSignupWhenNicknameIsBlocked() {
    String nickname = "관리자";

    SignupCommand command =
        new SignupCommand(
            "test@test.com",
            "verification-token",
            "safePassword123!",
            nickname,
            Map.of("TOS", true));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);
    when(nicknameBlocklistRepository.existsBlockedNickname(nickname)).thenReturn(true);

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("사용할 수 없는 닉네임입니다.");

    verify(passwordEncoder, never()).encode(any());
    verify(memberRepository, never()).save(any());
  }

  private record TestAgreement(Integer agreementId, String code, Boolean required)
      implements MemberAgreementRepository.CurrentAgreement {

    @Override
    public Integer getAgreementId() {
      return agreementId;
    }

    @Override
    public String getCode() {
      return code;
    }

    @Override
    public Boolean getRequired() {
      return required;
    }
  }

  @Test
  @DisplayName("사용 가능한 닉네임이면 true를 반환한다")
  void returnsTrueWhenNicknameIsAvailable() {
    String nickname = "새닉네임";

    boolean available = signupService.isNicknameAvailable(nickname);

    assertThat(available).isTrue();
    verify(nicknameBlocklistRepository).existsBlockedNickname(nickname);
    verify(memberRepository).existsByNormalizedNickname(nickname);
  }

  @Test
  @DisplayName("이미 사용 중인 닉네임이면 false를 반환한다")
  void returnsFalseWhenNicknameAlreadyExists() {
    String nickname = "더미사용자";

    when(memberRepository.existsByNormalizedNickname(nickname)).thenReturn(true);

    boolean available = signupService.isNicknameAvailable(nickname);

    assertThat(available).isFalse();
    verify(memberRepository).existsByNormalizedNickname(nickname);
  }

  @Test
  @DisplayName("금칙어 닉네임이면 400에 해당하는 예외를 발생시킨다")
  void rejectsBlockedNickname() {
    String nickname = "관리자";

    when(nicknameBlocklistRepository.existsBlockedNickname(nickname)).thenReturn(true);

    assertThatThrownBy(() -> signupService.isNicknameAvailable(nickname))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("사용할 수 없는 닉네임입니다.");

    verify(memberRepository, never()).existsByNormalizedNickname(any());
  }

  @Test
  @DisplayName("닉네임 길이가 2자 미만이면 거부한다")
  void rejectsTooShortNickname() {
    assertThatThrownBy(() -> signupService.isNicknameAvailable("가"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("닉네임은 2자 이상 8자 이하이어야 합니다.");

    verify(memberRepository, never()).existsByNormalizedNickname(any());
  }

  @Test
  @DisplayName("닉네임에 특수문자가 포함되면 거부한다")
  void rejectsNicknameWithSpecialCharacters() {
    assertThatThrownBy(() -> signupService.isNicknameAvailable("test!"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.");

    verify(memberRepository, never()).existsByNormalizedNickname(any());
  }

  @Test
  @DisplayName("회원가입 시 이미 사용 중인 닉네임이면 저장하지 않는다")
  void rejectsSignupWhenNicknameAlreadyExists() {
    String nickname = "더미사용자";

    SignupCommand command =
        new SignupCommand(
            "new@test.com",
            "verification-token",
            "safePassword123!",
            nickname,
            Map.of("TOS", true));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);
    when(memberRepository.existsByNormalizedNickname(nickname)).thenReturn(true);

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(BusinessException.class)
        .hasMessage("이미 사용 중인 닉네임입니다.");

    verify(memberRepository, never()).save(any(Member.class));
  }

  @Test
  @DisplayName("DB 닉네임 UNIQUE 제약 위반은 닉네임 중복 오류로 변환한다")
  void convertsNicknameUniqueViolationToBusinessException() {
    String nickname = "중복닉네임";

    SignupCommand command =
        new SignupCommand(
            "new@test.com",
            "verification-token",
            "safePassword123!",
            nickname,
            Map.of("TOS", true));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);
    when(passwordEncoder.encode("safePassword123!")).thenReturn("encoded-password");
    when(memberRepository.save(any(Member.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ConstraintViolationException constraintException =
        new ConstraintViolationException(
            "닉네임 중복", new SQLException("duplicate key", "23505"), "uk_member_nickname_normalized");

    doThrow(new DataIntegrityViolationException("DB UNIQUE 위반", constraintException))
        .when(memberRepository)
        .flush();

    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(BusinessException.class)
        .hasMessage("이미 사용 중인 닉네임입니다.");

    verify(memberRepository).flush();
    verify(memberRoleRepository, never()).save(any(MemberRole.class));
    verify(memberAgreementRepository, never()).save(any(MemberAgreement.class));
  }

  @Test
  @DisplayName("닉네임과 무관한 DB 제약 위반은 닉네임 중복 오류로 변환하지 않는다")
  void doesNotConvertUnrelatedConstraintViolation() {
    SignupCommand command =
        new SignupCommand(
            "new@test.com", "verification-token", "safePassword123!", "새닉네임", Map.of("TOS", true));

    when(memberAgreementRepository.findCurrentAgreements()).thenReturn(CURRENT_AGREEMENTS);
    when(passwordEncoder.encode("safePassword123!")).thenReturn("encoded-password");
    when(memberRepository.save(any(Member.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ConstraintViolationException constraintException =
        new ConstraintViolationException(
            "다른 DB 제약 위반",
            new SQLException("constraint violation", "23503"),
            "fk_member_preferred_region");

    DataIntegrityViolationException dbException =
        new DataIntegrityViolationException("DB 제약 위반", constraintException);

    doThrow(dbException).when(memberRepository).flush();

    assertThatThrownBy(() -> signupService.createLocalMember(command)).isSameAs(dbException);

    verify(memberRepository).flush();
    verify(memberRoleRepository, never()).save(any(MemberRole.class));
  }
}
