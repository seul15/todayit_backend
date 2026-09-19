package com.todayit.member.service;

import com.todayit.common.auth.service.LoginAttemptService;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberProvider;
import com.todayit.member.exception.LoginFailedException;
import com.todayit.member.exception.LoginLockedException;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.command.LoginCommand;
import com.todayit.member.service.model.MemberLoginResult;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 로컬 회원의 로그인 인증을 처리합니다. */
@Service
public class MemberLoginService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final LoginAttemptService loginAttemptService;

  private static final String DUMMY_PASSWORD_HASH =
      "$2a$10$6nCpOmOInixYvfnvy.pM7OZaJ4enUex4OpI4Z19SIcrSNxXJehyfm";

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
   * @param command 로그인 서비스에 전달된 이메일과 비밀번호
   * @return 인증에 성공한 회원 정보
   * @throws LoginFailedException 로그인 정보가 올바르지 않을 때
   * @throws LoginLockedException 로그인 시도 횟수를 초과해 잠긴 상태일 때
   * @throws IllegalStateException 회원에게 유효한 권한이 없을 때
   */
  public MemberLoginResult login(LoginCommand command) {
    // LoginCommand에서 이메일과 비밀번호 확인
    String email = command.email();
    String password = command.password();

    // 같은 이메일의 소셜 계정과 구분하여 LOCAL 회원만 조회
    Member member =
        memberRepository.findByEmailAndProvider(email, MemberProvider.LOCAL).orElse(null);

    if (member == null) {
      // 회원이 없어도 BCrypt 검증을 수행해 로그인 응답 시간 차이를 줄임
      performDummyPasswordCheck(password);
      throw new LoginFailedException();
    }

    // 탈퇴 등으로 비활성화된 회원은 로그인 실패
    if (!member.isActive()) {
      // 비활성 회원도 BCrypt 검증을 수행해 계정 상태에 따른 응답 시간 차이를 줄임
      performDummyPasswordCheck(password);
      throw new LoginFailedException();
    }

    // 로그인 실패 횟수가 5회 이상이면 로그인 차단
    if (loginAttemptService.isLocked(email)) {
      throw new LoginLockedException();
    }

    // 비밀번호 확인
    // 틀릴 경우 실패 횟수+1 -> 5회가 되면 계정 잠금
    if (!passwordEncoder.matches(password, member.getPassword())) {
      long failureCount = loginAttemptService.recordFailure(email);

      if (failureCount >= 5) {
        throw new LoginLockedException();
      }

      throw new LoginFailedException(failureCount);
    }
    // 로그인 회원의 권한 확인
    List<String> roles = memberRepository.findRoleNamesByMemberId(member.getId());

    if (roles.isEmpty()) {
      throw new IllegalStateException("회원 권한 정보가 없습니다.");
    }

    // 로그인 성공 시 로그인 실패 횟수 초기화
    loginAttemptService.resetFailures(email);

    // 인증 완료된 회원 정보 리턴
    return new MemberLoginResult(member.getId(), roles);
  }

  private void performDummyPasswordCheck(String password) {
    passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
  }
}
