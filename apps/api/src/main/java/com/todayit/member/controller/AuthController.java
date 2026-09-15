package com.todayit.member.controller;

import com.todayit.member.dto.request.LoginRequest;
import com.todayit.member.dto.response.LoginResponse;
import com.todayit.member.service.LoginFacade;
import com.todayit.member.service.model.LoginResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인과 토큰 발급 관련 API를 처리합니다. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final LoginFacade loginFacade;

  /**
   * 로그인 처리를 담당하는 Facade를 받습니다.
   *
   * @param loginFacade 로그인 처리 Facade
   */
  public AuthController(LoginFacade loginFacade) {
    this.loginFacade = loginFacade;
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
}
