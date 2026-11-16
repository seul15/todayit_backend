package com.todayit.place.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todayit.common.pagination.PageResponse;
import com.todayit.common.pagination.PaginationValidator;
import com.todayit.common.response.CommonResponse;
import com.todayit.course.dto.response.CourseResponse;
import com.todayit.course.exception.InvalidRegionPathException;
import com.todayit.course.service.CoursePlaceQueryService;
import com.todayit.course.service.model.CourseSort;
import com.todayit.course.service.model.RegionFilter;
import com.todayit.place.controller.docs.PlaceApiDocs;
import com.todayit.place.dto.request.RegionFilterRequest;
import com.todayit.place.dto.response.PlaceDetailResponse;
import com.todayit.place.dto.response.PlaceFilterResponse;
import com.todayit.place.dto.response.PlaceImageResponse;
import com.todayit.place.dto.response.PlaceLikeResponse;
import com.todayit.place.dto.response.PlaceLocationResponse;
import com.todayit.place.dto.response.PlaceScrapResponse;
import com.todayit.place.entity.Category;
import com.todayit.place.exception.ExceededSigunguLimitException;
import com.todayit.place.exception.InvalidPlaceCategoryFilterException;
import com.todayit.place.service.PlaceCommandService;
import com.todayit.place.service.PlaceDetailFacade;
import com.todayit.place.service.PlaceQueryService;
import com.todayit.place.service.model.PlaceSort;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
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
public class PlaceController implements PlaceApiDocs {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIGUNGU_COUNT_PER_SIDO = 3;
  private static final Set<String> CATEGORY_FILTER_VALUES =
      Arrays.stream(Category.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());

  private final PlaceDetailFacade placeDetailFacade;
  private final PlaceQueryService placeQueryService;
  private final PlaceCommandService placeCommandService;
  private final CoursePlaceQueryService coursePlaceQueryService;
  private final ObjectMapper objectMapper;

  /**
   * 장소 기능을 처리할 서비스를 받습니다.
   *
   * @param placeQueryService 장소 조회 Service
   * @param placeCommandService 장소 변경 Service
   * @param coursePlaceQueryService 코스 장소 조회 Service 계약
   * @param placeDetailFacade 장소 상세 조회 Facade
   */
  public PlaceController(
      PlaceQueryService placeQueryService,
      PlaceCommandService placeCommandService,
      CoursePlaceQueryService coursePlaceQueryService,
      PlaceDetailFacade placeDetailFacade) {
    this.placeDetailFacade = placeDetailFacade;
    this.placeQueryService = placeQueryService;
    this.placeCommandService = placeCommandService;
    this.coursePlaceQueryService = coursePlaceQueryService;
    this.objectMapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  }

  /**
   * 장소 상세를 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 선택 인증 정보
   * @return 장소 상세 응답
   */
  @GetMapping("/{placeId}")
  public ResponseEntity<CommonResponse<PlaceDetailResponse>> getPlaceDetail(
      @PathVariable int placeId, Authentication authentication) {
    String memberId = authentication == null ? null : authentication.getName();
    return ResponseEntity.ok(
        CommonResponse.success(
            PlaceDetailResponse.from(placeDetailFacade.getPlaceDetail(placeId, memberId))));
  }

  /**
   * 활성화되고 삭제되지 않은 장소 목록을 조회합니다.
   *
   * @param regions 지역 필터 JSON 문자열, 생략 가능
   * @param categories 장소 카테고리 목록
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 정렬 기준
   * @param authentication 선택 인증 정보
   * @return 장소 목록과 페이지 정보
   */
  @GetMapping
  public ResponseEntity<CommonResponse<PageResponse<PlaceFilterResponse>>> findPlaces(
      @RequestParam(required = false) String regions,
      @RequestParam(required = false) List<String> categories,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
      @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
      @RequestParam(defaultValue = "LATEST") PlaceSort sort,
      Authentication authentication) {
    PaginationValidator.validate(page, size);
    List<RegionFilter> regionFilters = parseRegions(regions);
    List<Category> categoryFilters = parseCategories(categories);
    return ResponseEntity.ok(
        CommonResponse.success(
            PageResponse.from(
                placeQueryService.findPlaces(
                    regionFilters,
                    categoryFilters,
                    page,
                    size,
                    sort,
                    authentication == null ? null : authentication.getName()),
                PlaceFilterResponse::from)));
  }

  /**
   * 가게 이름으로 장소를 검색합니다.
   *
   * @param keyword 가게 이름 검색어
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param authentication 선택 인증 정보
   * @return 검색 결과와 페이지 정보
   */
  @GetMapping("/search")
  public ResponseEntity<CommonResponse<PageResponse<PlaceFilterResponse>>> searchPlaces(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
      @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
      Authentication authentication) {
    PaginationValidator.validate(page, size);
    return ResponseEntity.ok(
        CommonResponse.success(
            PageResponse.from(
                placeQueryService.searchPlaces(
                    keyword, page, size, authentication == null ? null : authentication.getName()),
                PlaceFilterResponse::from)));
  }

  /**
   * 지역 필터 JSON 문자열을 파싱하고 검증합니다.
   *
   * @param regionsJson 지역 필터 JSON 문자열
   * @return 변환한 지역 필터 목록, 생략 시 {@code null}
   * @throws InvalidRegionPathException 형식이 잘못되었거나 코드가 올바르지 않을 때
   * @throws ExceededSigunguLimitException 시도당 시군구 코드가 3개를 초과할 때
   */
  private List<RegionFilter> parseRegions(String regionsJson) {
    if (regionsJson == null) {
      return null;
    }
    if (regionsJson.isBlank()) {
      throw new InvalidRegionPathException();
    }

    List<RegionFilterRequest> requests;
    try {
      requests =
          objectMapper.readValue(regionsJson, new TypeReference<List<RegionFilterRequest>>() {});
    } catch (Exception e) {
      throw new InvalidRegionPathException();
    }

    if (requests == null || requests.isEmpty()) {
      throw new InvalidRegionPathException();
    }

    Map<String, Set<String>> sigunguBySido = new LinkedHashMap<>();
    for (RegionFilterRequest request : requests) {
      if (request == null
          || request.sidoCode() == null
          || !request.sidoCode().matches("^[0-9]{2}$")) {
        throw new InvalidRegionPathException();
      }

      if (sigunguBySido.containsKey(request.sidoCode())) {
        throw new InvalidRegionPathException();
      }

      List<String> sigunguCodes = request.sigunguCodes();
      Set<String> set = new LinkedHashSet<>();
      if (sigunguCodes != null) {
        for (String sigunguCode : sigunguCodes) {
          if (sigunguCode == null || !sigunguCode.matches("^[0-9]{3}$")) {
            throw new InvalidRegionPathException();
          }
          set.add(sigunguCode);
        }
        if (set.size() > MAX_SIGUNGU_COUNT_PER_SIDO) {
          throw new ExceededSigunguLimitException();
        }
      }

      sigunguBySido.put(request.sidoCode(), set);
    }

    return sigunguBySido.entrySet().stream()
        .map(entry -> new RegionFilter(entry.getKey(), List.copyOf(entry.getValue())))
        .toList();
  }

  /**
   * 요청 카테고리 값을 장소 카테고리 목록으로 변환합니다.
   *
   * @param categories 요청 카테고리 값 목록
   * @return 변환한 장소 카테고리 목록, 파라미터를 생략하면 {@code null}
   * @throws InvalidPlaceCategoryFilterException 빈 값이거나 지원하지 않는 카테고리일 때
   */
  private List<Category> parseCategories(List<String> categories) {
    if (categories == null) {
      return null;
    }
    if (categories.isEmpty()
        || categories.stream()
            .anyMatch(
                category -> category.isBlank() || !CATEGORY_FILTER_VALUES.contains(category))) {
      throw new InvalidPlaceCategoryFilterException();
    }

    return categories.stream().map(Category::valueOf).toList();
  }

  /**
   * 장소의 지도 표시 정보를 조회합니다.
   *
   * @param placeId 장소 식별자
   * @return 장소 위치 응답
   */
  @GetMapping("/{placeId}/location")
  public ResponseEntity<CommonResponse<PlaceLocationResponse>> findPlaceLocation(
      @PathVariable int placeId) {
    return ResponseEntity.ok(
        CommonResponse.success(
            PlaceLocationResponse.from(placeQueryService.findPlaceLocation(placeId))));
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
  public ResponseEntity<CommonResponse<PageResponse<PlaceImageResponse>>> findPlaceImages(
      @PathVariable int placeId,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
      @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
    PaginationValidator.validate(page, size);
    return ResponseEntity.ok(
        CommonResponse.success(
            PageResponse.from(
                placeQueryService.findPlaceImages(placeId, page, size), PlaceImageResponse::from)));
  }

  /**
   * 특정 장소를 포함한 공개 코스 목록을 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 코스 정렬 기준
   * @return 코스 목록과 페이지 정보
   */
  @GetMapping("/{placeId}/courses")
  public ResponseEntity<CommonResponse<PageResponse<CourseResponse>>> findCoursesByPlace(
      @PathVariable int placeId,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
      @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
      @RequestParam(defaultValue = "LATEST") CourseSort sort) {
    PaginationValidator.validate(page, size);
    placeQueryService.validatePlaceExists(placeId);
    return ResponseEntity.ok(
        CommonResponse.success(
            PageResponse.from(
                coursePlaceQueryService.findCoursesByPlace(placeId, page, size, sort),
                CourseResponse::from)));
  }

  /**
   * 인증된 회원의 장소 스크랩을 생성합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 인증된 회원 정보
   * @return 장소 스크랩 결과
   */
  @PostMapping("/{placeId}/scrap")
  public ResponseEntity<CommonResponse<PlaceScrapResponse>> scrapPlace(
      @PathVariable int placeId, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            CommonResponse.success(
                PlaceScrapResponse.from(
                    placeCommandService.scrapPlace(placeId, authentication.getName()))));
  }

  /**
   * 인증된 회원의 장소 스크랩을 취소합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 인증된 회원 정보
   * @return 장소 스크랩 결과
   */
  @DeleteMapping("/{placeId}/scrap")
  public ResponseEntity<CommonResponse<PlaceScrapResponse>> cancelPlaceScrap(
      @PathVariable int placeId, Authentication authentication) {
    return ResponseEntity.ok(
        CommonResponse.success(
            PlaceScrapResponse.from(
                placeCommandService.cancelPlaceScrap(placeId, authentication.getName()))));
  }

  /**
   * 인증된 회원의 장소 좋아요를 생성합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 인증된 회원 정보
   * @return 장소 좋아요 결과
   */
  @PostMapping("/{placeId}/like")
  public ResponseEntity<CommonResponse<PlaceLikeResponse>> likePlace(
      @PathVariable int placeId, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            CommonResponse.success(
                PlaceLikeResponse.from(
                    placeCommandService.likePlace(placeId, authentication.getName()))));
  }
}
