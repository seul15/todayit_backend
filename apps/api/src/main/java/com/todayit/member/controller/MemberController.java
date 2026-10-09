package com.todayit.member.controller;

import com.todayit.common.dto.response.ApiResponse;
import com.todayit.member.dto.response.NicknameAvailabilityResponse;
import com.todayit.member.service.SignupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 회원 관련 API를 처리합니다. */
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

  private final SignupService signupService;

  /**
   * 회원 API에 필요한 서비스를 받습니다.
   *
   * @param signupService 회원가입 관련 서비스
   */
  public MemberController(SignupService signupService) {
    this.signupService = signupService;
  }

  /**
   * 닉네임 사용 가능 여부를 확인합니다.
   *
   * @param nickname 확인할 닉네임
   * @return 닉네임 사용 가능 여부
   */
  @GetMapping("/nickname/check")
  public ResponseEntity<ApiResponse<NicknameAvailabilityResponse>> checkNickname(
      @RequestParam(required = false) String nickname) {

    boolean available = signupService.isNicknameAvailable(nickname);

    return ResponseEntity.ok(ApiResponse.success(new NicknameAvailabilityResponse(available)));
  }
}
