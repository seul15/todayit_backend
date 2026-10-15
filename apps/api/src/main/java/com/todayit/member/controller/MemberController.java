package com.todayit.member.controller;

import com.todayit.common.pagination.PageResponse;
import com.todayit.common.pagination.PaginationValidator;
import com.todayit.common.response.CommonResponse;
import com.todayit.member.controller.docs.MemberApiDocs;
import com.todayit.member.dto.response.NicknameAvailabilityResponse;
import com.todayit.member.service.SignupService;
import com.todayit.place.dto.response.PlaceResponse;
import com.todayit.place.service.PlaceScrapQueryService;
import com.todayit.place.service.model.PlaceScrapSort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 회원 관련 API를 처리합니다. */
@RestController
@RequestMapping("/api/v1/members")
public class MemberController implements MemberApiDocs {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;

  private final PlaceScrapQueryService placeScrapQueryService;
  private final SignupService signupService;

  /**
   * 회원 관련 API에 필요한 서비스를 받습니다.
   *
   * @param signupService 회원가입 관련 서비스
   * @param placeScrapQueryService 장소 스크랩 조회 Service 계약
   */
  public MemberController(
      SignupService signupService, PlaceScrapQueryService placeScrapQueryService) {
    this.signupService = signupService;
    this.placeScrapQueryService = placeScrapQueryService;
  }

  /**
   * 닉네임 사용 가능 여부를 확인합니다.
   *
   * @param nickname 확인할 닉네임
   * @return 닉네임 사용 가능 여부
   */
  @GetMapping("/nickname/check")
  public ResponseEntity<CommonResponse<NicknameAvailabilityResponse>> checkNickname(
      @RequestParam(required = false) String nickname) {

    boolean available = signupService.isNicknameAvailable(nickname);

    return ResponseEntity.ok(CommonResponse.success(new NicknameAvailabilityResponse(available)));
  }

  /**
   * 인증된 회원이 스크랩한 장소를 조회합니다.
   *
   * @param page 페이지 번호
   * @param sort 스크랩 장소 정렬 기준
   * @param authentication 인증된 회원 정보
   * @return 스크랩 장소 페이지
   */
  @GetMapping("/me/scrapped-places")
  public ResponseEntity<CommonResponse<PageResponse<PlaceResponse>>> findScrappedPlaces(
      @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
      @RequestParam(defaultValue = "LATEST") PlaceScrapSort sort,
      Authentication authentication) {
    PaginationValidator.validate(page, DEFAULT_SIZE);
    return ResponseEntity.ok(
        CommonResponse.success(
            PageResponse.from(
                placeScrapQueryService.findScrappedPlaces(
                    authentication.getName(), page, DEFAULT_SIZE, sort),
                PlaceResponse::from)));
  }
}
