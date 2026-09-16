package com.todayit.member.controller;

import com.todayit.member.dto.request.LoginRequest;
import com.todayit.member.dto.request.LogoutRequest;
import com.todayit.member.dto.response.LoginResponse;
import com.todayit.member.service.LoginFacade;
import com.todayit.member.service.LogoutService;
import com.todayit.member.service.model.LoginResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인과 토큰 발급 관련 API를 처리합니다. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final LoginFacade loginFacade;
  private final LogoutService logoutService;

  /**
   * 로그인과 로그아웃 처리를 담당하는 서비스를 받습니다.
   *
   * @param loginFacade 로그인 처리 Facade
   * @param logoutService 로그아웃 처리 Service
   */
  public AuthController(LoginFacade loginFacade, LogoutService logoutService) {
    this.loginFacade = loginFacade;
    this.logoutService = logoutService;
  }

  /**
   * 이메일과 비밀번호로 로그인합니다.
   *
   * @param request 로그인 요청
   * @return 로그인 성공 정보와 토큰
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    // 이메일, 비밀번호 입력됐는지 확인
    request.validate();

    // 로그인 요청을 서비스에서 사용할 형태로 변환
    LoginResult result = loginFacade.login(request.toCommand());

    // 로그인 결과를 API 응답으로 리턴
    return ResponseEntity.ok(LoginResponse.from(result));
  }

  /**
   * 현재 기기의 로그인 세션을 종료합니다.
   *
   * @param authentication 로그인한 회원 인증 정보
   * @param request 로그아웃 요청
   * @return 응답 본문 없음
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      Authentication authentication, @RequestBody LogoutRequest request) {

    // Refresh Token이 입력됐는지 확인
    request.validate();

    // Access Token 인증으로 확인된 회원 ID 가져오기
    String memberId = authentication.getName();

    // 현재 기기의 로그인 세션만 종료
    logoutService.logout(memberId, request.refreshToken());

    // 로그아웃 완료
    return ResponseEntity.noContent().build();
  }
}
