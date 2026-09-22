package com.todayit.member.service;

import com.todayit.member.entity.MemberProvider;
import com.todayit.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 회원가입 관련 기능을 처리합니다. */
@Service
public class SignupService {

  private final MemberRepository memberRepository;

  /**
   * 회원가입에 필요한 회원 저장소를 받습니다.
   *
   * @param memberRepository 회원 Repository
   */
  public SignupService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
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
}
