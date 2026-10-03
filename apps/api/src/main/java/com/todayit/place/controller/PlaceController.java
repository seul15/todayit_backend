package com.todayit.place.controller;

import com.todayit.common.pagination.PageResponse;
import com.todayit.common.pagination.PaginationValidator;
import com.todayit.common.response.ApiResponse;
import com.todayit.place.dto.response.PlaceImageResponse;
import com.todayit.place.dto.response.PlaceLocationResponse;
import com.todayit.place.dto.response.PlaceResponse;
import com.todayit.place.dto.response.PlaceScrapResponse;
import com.todayit.place.service.PlaceService;
import com.todayit.place.service.model.PlaceSort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 장소 관련 HTTP 요청을 처리하는 Controller입니다. */
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;

  private final PlaceService placeService;

  /**
   * 장소 기능을 처리할 서비스를 받습니다.
   *
   * @param placeService 장소 Service
   */
  public PlaceController(PlaceService placeService) {
    this.placeService = placeService;
  }

  /**
   * 활성화되고 삭제되지 않은 장소 목록을 조회합니다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 정렬 기준
   * @return 장소 목록과 페이지 정보
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<PlaceResponse>>> findPlaces(
      @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
      @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
      @RequestParam(defaultValue = "LATEST") PlaceSort sort) {
    PaginationValidator.validate(page, size);
    return ResponseEntity.ok(
        ApiResponse.success(
            PageResponse.from(placeService.findPlaces(page, size, sort), PlaceResponse::from)));
  }

  /**
   * 장소의 지도 표시 정보를 조회합니다.
   *
   * @param placeId 장소 식별자
   * @return 장소 위치 응답
   */
  @GetMapping("/{placeId}/location")
  public ResponseEntity<ApiResponse<PlaceLocationResponse>> findPlaceLocation(
      @PathVariable int placeId) {
    return ResponseEntity.ok(
        ApiResponse.success(PlaceLocationResponse.from(placeService.findPlaceLocation(placeId))));
  }

  /**
   * 장소의 사진 목록을 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 장소 사진 목록과 페이지 정보
   */
  @GetMapping("/{placeId}/images")
  public ResponseEntity<ApiResponse<PageResponse<PlaceImageResponse>>> findPlaceImages(
      @PathVariable int placeId,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
      @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
    PaginationValidator.validate(page, size);
    return ResponseEntity.ok(
        ApiResponse.success(
            PageResponse.from(
                placeService.findPlaceImages(placeId, page, size), PlaceImageResponse::from)));
  }

  /**
   * 인증된 회원의 장소 스크랩을 생성합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 인증된 회원 정보
   * @return 장소 스크랩 결과
   */
  @PostMapping("/{placeId}/scrap")
  public ResponseEntity<ApiResponse<PlaceScrapResponse>> scrapPlace(
      @PathVariable int placeId, Authentication authentication) {
    return ResponseEntity.ok(
        ApiResponse.success(
            PlaceScrapResponse.from(placeService.scrapPlace(placeId, authentication.getName()))));
  }

  /**
   * 인증된 회원의 장소 스크랩을 취소합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 인증된 회원 정보
   * @return 장소 스크랩 결과
   */
  @DeleteMapping("/{placeId}/scrap")
  public ResponseEntity<ApiResponse<PlaceScrapResponse>> cancelPlaceScrap(
      @PathVariable int placeId, Authentication authentication) {
    return ResponseEntity.ok(
        ApiResponse.success(
            PlaceScrapResponse.from(
                placeService.cancelPlaceScrap(placeId, authentication.getName()))));
  }
}
