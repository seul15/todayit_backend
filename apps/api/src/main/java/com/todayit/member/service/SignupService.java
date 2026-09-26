package com.todayit.member.service;

import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 회원가입 관련 기능을 처리합니다. */
@Service
public class SignupService {

  private final MemberRepository memberRepository;

  private final PasswordEncoder passwordEncoder;

  /**
   * 회원가입에 필요한 회원 저장소를 받습니다.
   *
   * @param memberRepository 회원 Repository
   * @param passwordEncoder 비밀번호 암호화
   */
  public SignupService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
    this.memberRepository = memberRepository;
    this.passwordEncoder = passwordEncoder;
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
   * @param password 평문 비밀번호
   * @param nickname 회원 닉네임
   * @return 저장된 회원
   */
  @Transactional
  public Member createLocalMember(String email, String password, String nickname) {
    String encodedPassword = passwordEncoder.encode(password);

    Member member = Member.createLocal(email, encodedPassword, nickname);

    return memberRepository.save(member);
  }
}
