package com.todayit.member.controller;

import com.todayit.common.dto.response.ApiResponse;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.dto.request.EmailVerificationConfirmRequest;
import com.todayit.member.dto.request.EmailVerificationRequest;
import com.todayit.member.dto.request.LoginRequest;
import com.todayit.member.dto.request.LogoutRequest;
import com.todayit.member.dto.request.SignupRequest;
import com.todayit.member.dto.request.TokenRefreshRequest;
import com.todayit.member.dto.response.EmailAvailabilityResponse;
import com.todayit.member.dto.response.EmailVerificationConfirmResponse;
import com.todayit.member.dto.response.EmailVerificationResponse;
import com.todayit.member.dto.response.LoginResponse;
import com.todayit.member.dto.response.SignupResponse;
import com.todayit.member.dto.response.TokenRefreshResponse;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.InvalidEmailVerificationCodeException;
import com.todayit.member.service.EmailVerificationService;
import com.todayit.member.service.LoginFacade;
import com.todayit.member.service.LogoutService;
import com.todayit.member.service.SignupService;
import com.todayit.member.service.TokenRefreshService;
import com.todayit.member.service.model.EmailVerificationConfirmResult;
import com.todayit.member.service.model.LoginResult;
import com.todayit.member.service.model.TokenRefreshResult;
import org.springframework.http.HttpStatus;
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
  private final EmailVerificationService emailVerificationService;
  private final TokenRefreshService tokenRefreshService;

  /**
   * 인증과 회원가입 처리에 필요한 서비스를 받습니다.
   *
   * @param loginFacade 로그인 처리 Facade
   * @param logoutService 로그아웃 처리 Service
   * @param signupService 회원가입 처리 Service
   * @param emailVerificationService 이메일 인증 처리 Service
   * @param tokenRefreshService 토큰 갱신 처리 Service
   */
  public AuthController(
      LoginFacade loginFacade,
      LogoutService logoutService,
      SignupService signupService,
      EmailVerificationService emailVerificationService,
      TokenRefreshService tokenRefreshService) {
    this.loginFacade = loginFacade;
    this.logoutService = logoutService;
    this.signupService = signupService;
    this.emailVerificationService = emailVerificationService;
    this.tokenRefreshService = tokenRefreshService;
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

  /**
   * 이메일 인증을 완료한 사용자를 로컬 회원으로 가입시킵니다.
   *
   * @param request 회원가입 요청
   * @return 생성된 회원 정보
   */
  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody SignupRequest request) {

    // 회원가입 필수 입력값 확인
    request.validate();

    // 회원 생성과 기본 권한 및 약관 동의 정보 저장
    Member member = signupService.createLocalMember(request.toCommand());

    // 회원가입 성공 응답 반환
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(SignupResponse.from(member)));
  }

  /**
   * 이메일 인증번호를 발급합니다.
   *
   * @param request 이메일 인증번호 발급 요청
   * @return 이메일 인증번호 유효 시간과 재발급 대기 시간
   */
  @PostMapping("/email-verifications")
  public ResponseEntity<ApiResponse<EmailVerificationResponse>> issueEmailVerification(
      @RequestBody EmailVerificationRequest request) {

    // 이메일과 인증 목적 입력값 확인
    request.validate();

    // 인증번호 생성 및 Redis 저장
    emailVerificationService.issueCode(request.email(), request.purpose());

    EmailVerificationResponse response =
        new EmailVerificationResponse(
            request.email(),
            request.purpose(),
            emailVerificationService.getCodeExpirationSeconds(),
            emailVerificationService.getResendCooldownSeconds());

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 이메일 인증번호를 확인하고 인증 완료 토큰을 발급합니다.
   *
   * @param request 이메일 인증번호 확인 요청
   * @return 이메일 인증 완료 토큰과 유효 시간
   * @throws InvalidEmailVerificationCodeException 인증번호 확인에 실패한 경우
   */
  @PostMapping("/email-verifications/confirm")
  public ResponseEntity<ApiResponse<EmailVerificationConfirmResponse>> confirmEmailVerification(
      @RequestBody EmailVerificationConfirmRequest request) {

    request.validate();

    EmailVerificationConfirmResult result =
        emailVerificationService.confirmCode(
            request.email(), request.purpose(), request.verificationCode());

    // 3회 실패 시 Service에서 새 인증번호를 재발급한 상태
    if (!result.verified()) {
      throw new InvalidEmailVerificationCodeException(3L);
    }

    EmailVerificationConfirmResponse response =
        new EmailVerificationConfirmResponse(
            result.verificationToken(),
            emailVerificationService.getVerificationTokenExpirationSeconds());

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Refresh Token으로 Access Token을 갱신합니다.
   *
   * @param request 토큰 갱신 요청
   * @return 갱신된 Access Token과 Refresh Token
   */
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
      @RequestBody TokenRefreshRequest request) {

    request.validate();

    TokenRefreshResult result = tokenRefreshService.refresh(request.refreshToken());

    return ResponseEntity.ok(ApiResponse.success(TokenRefreshResponse.from(result)));
  }
}
