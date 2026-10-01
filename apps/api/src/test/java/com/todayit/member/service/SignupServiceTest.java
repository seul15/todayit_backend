package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberAgreement;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.entity.MemberRole;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.repository.MemberRoleRepository;
import com.todayit.member.service.command.SignupCommand;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

  @Mock private MemberRepository memberRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private EmailVerificationService emailVerificationService;

  @Mock private MemberRoleRepository memberRoleRepository;

  @Mock private MemberAgreementRepository memberAgreementRepository;

  private SignupService signupService;

  @BeforeEach
  void setUp() {
    signupService =
        new SignupService(
            memberRepository,
            passwordEncoder,
            emailVerificationService,
            memberRoleRepository,
            memberAgreementRepository);
  }

  @Test
  @DisplayName("로컬 회원가입 시 회원과 USER 권한 및 약관 동의 정보를 저장한다")
  void createsLocalMemberWithEncodedPassword() {
    // Given
    String email = "test@test.com";
    String password = "password123!";
    String encodedPassword = "encoded-password";
    String verificationToken = "verification-token";
    String nickname = "테스트";
    Integer userRoleId = 1;

    Map<Integer, Boolean> agreements =
        Map.of(
            1, true,
            2, true,
            3, false,
            4, false);

    SignupCommand command =
        new SignupCommand(email, verificationToken, password, nickname, agreements);

    when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
    when(memberRepository.save(any(Member.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(memberRoleRepository.findRoleIdByName("USER")).thenReturn(Optional.of(userRoleId));
    when(memberRepository.existsByEmailAndProvider(email, MemberProvider.LOCAL)).thenReturn(false);
    when(memberAgreementRepository.findRequiredAgreementIds()).thenReturn(List.of(1, 2));

    // When
    Member savedMember = signupService.createLocalMember(command);

    // Then
    verify(emailVerificationService)
        .validateVerificationToken(email, EmailVerificationPurpose.SIGNUP, verificationToken);
    verify(emailVerificationService).invalidateVerificationToken(verificationToken);

    ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
    verify(memberRepository).save(memberCaptor.capture());

    Member member = memberCaptor.getValue();

    assertThat(member.getPassword()).isEqualTo(encodedPassword);
    assertThat(savedMember).isSameAs(member);
    assertThat(savedMember.getCreatedAt()).isNotNull();

    verify(memberRoleRepository).findRoleIdByName("USER");
    verify(memberRoleRepository).save(any(MemberRole.class));
    verify(memberAgreementRepository, times(agreements.size())).save(any(MemberAgreement.class));
  }

  @Test
  @DisplayName("이미 가입된 LOCAL 이메일이면 회원가입을 거부한다")
  void rejectsSignupWhenLocalEmailAlreadyExists() {
    // Given
    SignupCommand command =
        new SignupCommand(
            "test@test.com",
            "verification-token",
            "password123!",
            "테스트",
            Map.of(
                1, true,
                2, true,
                3, false,
                4, false));

    when(memberRepository.existsByEmailAndProvider("test@test.com", MemberProvider.LOCAL))
        .thenReturn(true);

    // When & Then
    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(BusinessException.class)
        .hasMessage("이미 사용 중인 이메일입니다.");

    verify(emailVerificationService, never()).validateVerificationToken(any(), any(), any());
    verify(memberRepository, never()).save(any());
  }

  @Test
  @DisplayName("필수 약관에 동의하지 않으면 회원가입을 거부한다")
  void rejectsSignupWhenRequiredAgreementIsNotAccepted() {
    // Given
    String email = "test@test.com";

    SignupCommand command =
        new SignupCommand(
            email,
            "verification-token",
            "password123!",
            "테스트",
            Map.of(
                1, true,
                2, false,
                3, false,
                4, false));

    when(memberRepository.existsByEmailAndProvider(email, MemberProvider.LOCAL)).thenReturn(false);

    when(memberAgreementRepository.findRequiredAgreementIds()).thenReturn(List.of(1, 2));

    // When & Then
    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("필수 약관과 동의 항목을 확인해 주세요.");

    verify(emailVerificationService, never()).validateVerificationToken(any(), any(), any());
    verify(emailVerificationService, never()).invalidateVerificationToken(any());

    verify(memberRepository, never()).save(any());
  }

  @Test
  @DisplayName("필수 약관 정보가 없으면 회원가입을 진행하지 않는다")
  void rejectsSignupWhenRequiredAgreementInformationDoesNotExist() {
    // Given
    String email = "test@test.com";

    SignupCommand command =
        new SignupCommand(email, "verification-token", "password123!", "테스트", Map.of(1, true));

    when(memberRepository.existsByEmailAndProvider(email, MemberProvider.LOCAL)).thenReturn(false);

    when(memberAgreementRepository.findRequiredAgreementIds()).thenReturn(List.of());

    // When & Then
    assertThatThrownBy(() -> signupService.createLocalMember(command))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("필수 약관 정보가 없습니다.");

    verify(emailVerificationService, never()).validateVerificationToken(any(), any(), any());

    verify(memberRepository, never()).save(any());
  }
}
