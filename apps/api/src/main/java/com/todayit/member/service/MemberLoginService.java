package com.todayit.member.service;

import com.todayit.common.auth.service.LoginAttemptService;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.exception.LoginFailedException;
import com.todayit.member.exception.LoginLockedException;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.model.MemberLoginResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 로컬 회원의 로그인 인증을 처리합니다. */
@Service
public class MemberLoginService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final LoginAttemptService loginAttemptService;

  /**
   * 로그인에 필요한 회원 저장소, 비밀번호 검증기, 로그인 시도 관리 서비스를 받습니다.
   *
   * @param memberRepository 회원 조회 Repository
   * @param passwordEncoder 비밀번호 검증기
   * @param loginAttemptService 로그인 실패 횟수와 잠금 상태 관리 Service
   */
  public MemberLoginService(
      MemberRepository memberRepository,
      PasswordEncoder passwordEncoder,
      LoginAttemptService loginAttemptService) {
    this.memberRepository = memberRepository;
    this.passwordEncoder = passwordEncoder;
    this.loginAttemptService = loginAttemptService;
  }

  /**
   * 이메일과 비밀번호로 로컬 회원을 인증합니다.
   *
   * @param email 로그인 이메일
   * @param password 사용자가 입력한 비밀번호
   * @return 인증에 성공한 회원 정보
   * @throws LoginFailedException 로그인 정보가 올바르지 않을 때
   * @throws LoginLockedException 로그인 시도 횟수를 초과해 잠긴 상태일 때
   */
  public MemberLoginResult login(String email, String password) {
    // 멘트 -> 5회 이상 틀려서 잠겼다.
    if (loginAttemptService.isLocked(email)) {
      throw new LoginLockedException();
    }

    Member member = memberRepository.findByEmail(email).orElseThrow(LoginFailedException::new);

    // 로컬 로그인인 경우
    if (member.getProvider() != MemberProvider.LOCAL) {
      throw new LoginFailedException();
    }

    // 활성 회원이 아닌 경우
    if (!member.isActive()) {
      throw new LoginFailedException();
    }

    // 비밀번호가 다른 경우
    if (!passwordEncoder.matches(password, member.getPassword())) {
      loginAttemptService.recordFailure(email);

      if (loginAttemptService.isLocked(email)) {
        throw new LoginLockedException();
      }

      throw new LoginFailedException();
    }

    // 로그인 성공 시 실패 횟수 초기화
    loginAttemptService.resetFailures(email);

    return new MemberLoginResult(member.getId());
  }
}
