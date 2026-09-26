package com.todayit.member.service;

import com.todayit.member.entity.EmailVerificationPurpose;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.entity.MemberRole;
import com.todayit.member.exception.InvalidEmailVerificationTokenException;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.repository.MemberRoleRepository;
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

  private static final String DEFAULT_ROLE_NAME = "USER";

  /**
   * 회원가입에 필요한 회원 저장소를 받습니다.
   *
   * @param memberRepository 회원 Repository
   * @param passwordEncoder 비밀번호 암호화
   * @param emailVerificationService 이메일 인증 Service
   * @param memberRoleRepository 회원 권한 Repository
   */
  public SignupService(
      MemberRepository memberRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationService emailVerificationService,
      MemberRoleRepository memberRoleRepository) {
    this.memberRepository = memberRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
    this.memberRoleRepository = memberRoleRepository;
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
   * 로컬 회원을 생성하고 저장합니다.
   *
   * @param email 회원 이메일
   * @param verificationToken 이메일 인증 완료 토큰
   * @param password 평문 비밀번호
   * @param nickname 회원 닉네임
   * @return 저장된 회원
   * @throws InvalidEmailVerificationTokenException 이메일 인증 정보가 유효하지 않은 경우
   */
  @Transactional
  public Member createLocalMember(
      String email, String verificationToken, String password, String nickname) {
    emailVerificationService.validateVerificationToken(
        email, EmailVerificationPurpose.SIGNUP, verificationToken);

    String encodedPassword = passwordEncoder.encode(password);

    Member member = Member.createLocal(email, encodedPassword, nickname);

    // 생성된 회원 정보를 DB에 저장
    Member savedMember = memberRepository.save(member);

    // 신규 회원에게 부여할 기본 USER 권한 식별자 조회
    Integer userRoleId =
        memberRoleRepository
            .findRoleIdByName(DEFAULT_ROLE_NAME)
            .orElseThrow(() -> new IllegalStateException("USER 권한 정보가 없습니다."));

    // 회원과 USER 권한 연결 정보 저장
    memberRoleRepository.save(MemberRole.create(savedMember.getId(), userRoleId));

    return savedMember;
  }
}
