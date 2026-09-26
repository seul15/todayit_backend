package com.todayit.member.controller;

import com.todayit.common.dto.response.ApiResponse;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.dto.request.LoginRequest;
import com.todayit.member.dto.request.LogoutRequest;
import com.todayit.member.dto.response.EmailAvailabilityResponse;
import com.todayit.member.dto.response.LoginResponse;
import com.todayit.member.service.LoginFacade;
import com.todayit.member.service.LogoutService;
import com.todayit.member.service.SignupService;
import com.todayit.member.service.model.LoginResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 인증과 회원가입 관련 API를 처리합니다. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final LoginFacade loginFacade;
  private final LogoutService logoutService;
  private final SignupService signupService;

  /**
   * 인증과 회원가입 처리에 필요한 서비스를 받습니다.
   *
   * @param loginFacade 로그인 처리 Facade
   * @param logoutService 로그아웃 처리 Service
   * @param signupService 회원가입 처리 Service
   */
  public AuthController(
      LoginFacade loginFacade, LogoutService logoutService, SignupService signupService) {
    this.loginFacade = loginFacade;
    this.logoutService = logoutService;
    this.signupService = signupService;
  }

  /**
   * 이메일과 비밀번호로 로그인합니다.
   *
   * @param request 로그인 요청
   * @return 로그인 성공 정보와 토큰
   */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
    // 이메일, 비밀번호 입력됐는지 확인
    request.validate();

    // 로그인 요청을 서비스에서 사용할 형태로 변환
    LoginResult result = loginFacade.login(request.toCommand());

    // 로그인 결과를 API 응답으로 리턴
    return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(result)));
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

  /**
   * 로컬 회원가입에 사용할 이메일의 중복 여부를 확인합니다.
   *
   * @param email 확인할 이메일
   * @return 이메일과 사용 가능 여부
   * @throws InvalidRequestException 이메일이 없거나 공백일 때
   */
  @GetMapping("/emails/check")
  public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmail(
      @RequestParam(required = false) String email) {

    if (email == null || email.isBlank()) {
      throw new InvalidRequestException("이메일은 필수입니다.");
    }

    boolean available = signupService.isEmailAvailable(email);

    return ResponseEntity.ok(ApiResponse.success(new EmailAvailabilityResponse(email, available)));
  }
}
