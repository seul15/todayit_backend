package com.todayit.place.controller.docs;

import com.todayit.common.openapi.OpenApiConfig;
import com.todayit.common.pagination.PageResponse;
import com.todayit.common.response.CommonResponse;
import com.todayit.common.response.ErrorResponse;
import com.todayit.course.dto.response.CourseResponse;
import com.todayit.course.service.model.CourseSort;
import com.todayit.place.dto.response.PlaceDetailResponse;
import com.todayit.place.dto.response.PlaceFilterResponse;
import com.todayit.place.dto.response.PlaceImageResponse;
import com.todayit.place.dto.response.PlaceLikeResponse;
import com.todayit.place.dto.response.PlaceLocationResponse;
import com.todayit.place.dto.response.PlaceScrapResponse;
import com.todayit.place.entity.Category;
import com.todayit.place.service.model.PlaceSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

/** 장소 API의 Swagger 문서를 정의하는 인터페이스입니다. */
@Tag(name = "장소", description = "장소 조회 및 상호작용 관련 API")
public interface PlaceApiDocs {

  /**
   * 가게 이름으로 장소를 검색합니다.
   *
   * @param keyword 가게 이름 검색어, 필수
   * @param page 0부터 시작하는 페이지 번호
   * @param size 페이지 크기
   * @param authentication 선택 인증 정보
   * @return 검색 결과와 페이지 정보
   */
  @Operation(
      summary = "가게 이름 검색",
      description =
          "가게 이름에 검색어의 글자가 하나 이상 포함된 활성 장소를 검색합니다. 공백과 영문 대소문자를 무시한 이름의 완전 일치를 우선합니다. 이후 글자 순서와 관계없이 글자별 검색어와 이름의 등장 횟수 중 작은 값을 합산하여 내림차순으로 정렬합니다. 동점이면 최신순, 장소 식별자 내림차순입니다. 저장된 이름은 원문 그대로 반환합니다. 빈 검색어는 빈 결과를 반환합니다. 인증은 선택사항이며 비회원의 liked는 false입니다.",
      security = {@SecurityRequirement(), @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "검색 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "검색어 누락 또는 잘못된 페이지 요청",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                  @ExampleObject(
                      name = "검색어 누락 또는 요청 형식 오류",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"),
                  @ExampleObject(
                      name = "잘못된 페이지 번호 또는 크기",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_PAGINATION\",\"message\":\"페이지 요청값이 올바르지 않습니다.\"}")
                })),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PageResponse<PlaceFilterResponse>>> searchPlaces(
      @Parameter(description = "가게 이름 검색어입니다.", example = "어니언 성수", required = true) String keyword,
      @Parameter(
              description = "페이지 번호입니다. 0부터 시작합니다.",
              example = "0",
              schema = @Schema(minimum = "0", defaultValue = "0"))
          int page,
      @Parameter(description = "페이지 크기입니다.", example = "20", schema = @Schema(defaultValue = "20"))
          int size,
      @Parameter(hidden = true) Authentication authentication);

  /**
   * 장소 식별자로 상세 정보를 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 선택 인증 정보
   * @return 장소 상세 응답
   */
  @Operation(
      summary = "장소 상세 조회",
      description =
          "장소의 기본 정보, 영업시간, 편의시설, 이미지 목록, 좋아요 여부, 스크랩 여부 등 상세 정보를 조회합니다. 인증은 선택사항이며, 비회원의 liked·scrapped는 false입니다.",
      security = {@SecurityRequirement(), @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"placeId\":1,\"name\":\"오늘 카페\",\"category\":\"CAFE_DESSERT\",\"address\":\"서울특별시 종로구 종로 1\",\"location\":{\"latitude\":37.57,\"longitude\":126.985},\"description\":\"조용한 분위기의 카페입니다.\",\"openingHours\":[{\"openDate\":\"2026-10-16T00:00:00\",\"startAt\":\"2026-10-16T09:00:00\",\"endAt\":\"2026-10-16T18:00:00\",\"breakStartAt\":\"2026-10-16T12:00:00\",\"breakEndAt\":\"2026-10-16T13:00:00\"}],\"images\":[\"https://example.com/images/place-1.jpg\"],\"averageStayMinutes\":60,\"liked\":true,\"scrapped\":false,\"visitorInfo\":{\"PARKING\":\"주차 가능\",\"PET_ACCESS\":\"반려동물 동반 불가\"}}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 장소 식별자",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"))),
    @ApiResponse(
        responseCode = "404",
        description = "장소가 없거나 비활성·삭제 상태",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_NOT_FOUND\",\"message\":\"장소를 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PlaceDetailResponse>> getPlaceDetail(
      @Parameter(description = "장소 식별자입니다.", example = "1", required = true) int placeId,
      @Parameter(hidden = true) Authentication authentication);

  /**
   * 장소 목록을 페이지 단위로 조회합니다.
   *
   * @param authentication 선택 인증 정보
   * @param regions 지역 필터 JSON 문자열, 생략 가능
   * @param categories 장소 카테고리 목록
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 정렬 기준
   * @return 장소 목록과 페이지 정보
   */
  @Operation(
      summary = "장소 필터 조회",
      description =
          "지역·카테고리로 활성 장소를 페이지 단위로 조회합니다. regions는 시도 및 시군구 목록을 담은 JSON 배열 문자열이며 시도당 시군구는 최대 3개까지 지정 가능합니다. 생략 시 전체 지역을 조회합니다. 인증은 선택사항이며, 비회원의 liked는 false입니다.",
      security = {@SecurityRequirement(), @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"content\":[{\"placeId\":1,\"name\":\"오늘의 식당\",\"latitude\":37.57000000,\"longitude\":126.98500000,\"address\":\"서울특별시 종로구 종로 1\",\"category\":\"RESTAURANT\",\"viewCount\":15,\"liked\":false,\"imageUrls\":[\"https://example.com/place.jpg\"]}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 필터 또는 페이지 요청",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                  @ExampleObject(
                      name = "잘못된 카테고리",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_PLACE_CATEGORY\",\"message\":\"카테고리 필터 값이 올바르지 않습니다.\"}"),
                  @ExampleObject(
                      name = "잘못된 지역 코드",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REGION_PATH\",\"message\":\"지역 필터 값이 올바르지 않습니다.\"}"),
                  @ExampleObject(
                      name = "시군구 제한 초과",
                      value =
                          "{\"success\":false,\"code\":\"EXCEEDED_SIGUNGU_LIMIT\",\"message\":\"시군구 필터는 시도당 최대 3개까지 선택할 수 있습니다.\"}"),
                  @ExampleObject(
                      name = "잘못된 페이지",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_PAGINATION\",\"message\":\"페이지 요청값이 올바르지 않습니다.\"}")
                })),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PageResponse<PlaceFilterResponse>>> findPlaces(
      @Parameter(
              description =
                  "지역 필터 JSON 배열 문자열입니다. 시도당 시군구 코드는 최대 3개까지 지정할 수 있으며, 빈 배열이거나 생략하면 해당 시도 전체를 조회합니다.",
              example =
                  "[{\"sidoCode\":\"11\",\"sigunguCodes\":[\"680\",\"650\"]},{\"sidoCode\":\"41\",\"sigunguCodes\":[]}]")
          String regions,
      @Parameter(
              description =
                  "카테고리를 복수 선택할 수 있습니다. 쉼표로 구분하며, 선택한 값 중 하나에 해당하는 장소를 조회합니다. 생략하면 전체 카테고리를 조회합니다.",
              examples = {
                @ExampleObject(name = "음식점", value = "[\"RESTAURANT\"]"),
                @ExampleObject(name = "카페·디저트", value = "[\"CAFE_DESSERT\"]"),
                @ExampleObject(name = "전시·박물관", value = "[\"EXHIBITION_MUSEUM\"]"),
                @ExampleObject(name = "공연", value = "[\"PERFORMANCE\"]"),
                @ExampleObject(name = "체험", value = "[\"EXPERIENCE\"]"),
                @ExampleObject(name = "놀이·레저", value = "[\"PLAY_LEISURE\"]"),
                @ExampleObject(name = "자연·산책", value = "[\"NATURE_WALK\"]"),
                @ExampleObject(name = "쇼핑", value = "[\"SHOPPING\"]"),
                @ExampleObject(name = "명소·전망", value = "[\"LANDMARK_VIEW\"]"),
                @ExampleObject(name = "음식점 + 카페·디저트", value = "[\"RESTAURANT\",\"CAFE_DESSERT\"]"),
                @ExampleObject(
                    name = "전시·박물관 + 공연",
                    value = "[\"EXHIBITION_MUSEUM\",\"PERFORMANCE\"]"),
                @ExampleObject(
                    name = "자연·산책 + 명소·전망",
                    value = "[\"NATURE_WALK\",\"LANDMARK_VIEW\"]")
              },
              array = @ArraySchema(schema = @Schema(implementation = Category.class)))
          List<String> categories,
      @Parameter(
              description = "페이지 번호입니다. 0부터 시작합니다.",
              example = "0",
              schema = @Schema(minimum = "0"))
          int page,
      @Parameter(
              description = "페이지당 결과 개수입니다. 1부터 100까지 가능합니다.",
              example = "20",
              schema = @Schema(minimum = "1", maximum = "100"))
          int size,
      @Parameter(
              description = "정렬 기준입니다. LATEST(최신순), POPULAR(조회수순) 중 선택합니다. 생략하면 LATEST입니다.",
              example = "LATEST")
          PlaceSort sort,
      @Parameter(hidden = true) Authentication authentication);

  /**
   * 장소의 지도 표시 정보를 조회합니다.
   *
   * @param placeId 장소 식별자
   * @return 장소 위치 응답
   */
  @Operation(summary = "장소 위치 조회", description = "장소의 식별자, 위도·경도 좌표와 주소를 조회합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"placeId\":1,\"latitude\":37.57000000,\"longitude\":126.98500000,\"address\":\"서울특별시 종로구 종로 1\"}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 장소 식별자",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"))),
    @ApiResponse(
        responseCode = "404",
        description = "장소가 없거나 비활성·삭제 상태",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_NOT_FOUND\",\"message\":\"장소를 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PlaceLocationResponse>> findPlaceLocation(
      @Parameter(description = "장소 식별자입니다.", example = "1", required = true) int placeId);

  /**
   * 장소 사진 목록을 페이지 단위로 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 장소 사진 목록과 페이지 정보
   */
  @Operation(summary = "장소 사진 목록 조회", description = "장소에 등록된 사진을 최신 등록순으로 페이지 단위 조회합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"content\":[{\"placeImageId\":1,\"imageUrl\":\"https://example.com/place.jpg\"}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 장소 식별자 또는 페이지 요청",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                  @ExampleObject(
                      name = "잘못된 식별자",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"),
                  @ExampleObject(
                      name = "잘못된 페이지",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_PAGINATION\",\"message\":\"페이지 요청값이 올바르지 않습니다.\"}")
                })),
    @ApiResponse(
        responseCode = "404",
        description = "장소가 없거나 비활성·삭제 상태",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_NOT_FOUND\",\"message\":\"장소를 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PageResponse<PlaceImageResponse>>> findPlaceImages(
      @Parameter(description = "장소 식별자입니다.", example = "1", required = true) int placeId,
      @Parameter(
              description = "페이지 번호입니다. 0부터 시작합니다.",
              example = "0",
              schema = @Schema(minimum = "0"))
          int page,
      @Parameter(
              description = "페이지당 결과 개수입니다. 1부터 100까지 가능합니다.",
              example = "20",
              schema = @Schema(minimum = "1", maximum = "100"))
          int size);

  /**
   * 특정 장소를 포함한 공개 코스 목록을 페이지 단위로 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 코스 정렬 기준
   * @return 코스 목록과 페이지 정보
   */
  @Operation(
      summary = "장소 포함 코스 목록 조회",
      description = "특정 장소를 경유하는 공개 코스 목록을 페이지 단위로 조회합니다. 장소가 없거나 비활성·삭제 상태이면 404를 반환합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"content\":[{\"courseId\":1,\"title\":\"주말 데이트 코스\",\"description\":\"분위기 좋은 카페와 산책로를 엮은 코스입니다.\",\"places\":[{\"coursePlaceId\":1,\"placeId\":10,\"order\":1,\"durationMinute\":60,\"name\":\"성수 카페\",\"category\":\"CAFE_DESSERT\",\"address\":\"서울특별시 성동구 성수동 1가\",\"latitude\":37.54450000,\"longitude\":127.05600000}],\"viewCount\":120,\"bookmarkCount\":15,\"bookmarked\":false}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 장소 식별자 또는 페이지 요청",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                  @ExampleObject(
                      name = "잘못된 식별자",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"),
                  @ExampleObject(
                      name = "잘못된 페이지",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_PAGINATION\",\"message\":\"페이지 요청값이 올바르지 않습니다.\"}")
                })),
    @ApiResponse(
        responseCode = "404",
        description = "장소가 없거나 비활성·삭제 상태",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_NOT_FOUND\",\"message\":\"장소를 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PageResponse<CourseResponse>>> findCoursesByPlace(
      @Parameter(description = "장소 식별자입니다.", example = "1", required = true) int placeId,
      @Parameter(
              description = "페이지 번호입니다. 0부터 시작합니다.",
              example = "0",
              schema = @Schema(minimum = "0"))
          int page,
      @Parameter(
              description = "페이지당 결과 개수입니다. 1부터 100까지 가능합니다.",
              example = "20",
              schema = @Schema(minimum = "1", maximum = "100"))
          int size,
      @Parameter(
              description =
                  "코스 정렬 기준입니다. LATEST(최신순), POPULAR(조회수순), BOOKMARK(스크랩순) 중 선택합니다. 생략하면 LATEST입니다.",
              example = "LATEST")
          CourseSort sort);

  /**
   * 장소 스크랩을 생성합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 인증된 회원 정보
   * @return 장소 스크랩 결과
   */
  @Operation(
      summary = "장소 스크랩 등록",
      description = "인증된 회원이 장소를 스크랩합니다. 이미 스크랩한 장소이면 400을 반환합니다.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "스크랩 생성 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"placeId\":1,\"scrapped\":true,\"scrapCount\":1}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "이미 스크랩된 장소",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_ALREADY_SCRAPPED\",\"message\":\"이미 스크랩되었습니다.\"}"))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}"))),
    @ApiResponse(
        responseCode = "404",
        description = "장소가 없거나 비활성·삭제 상태",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_NOT_FOUND\",\"message\":\"장소를 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PlaceScrapResponse>> scrapPlace(
      @Parameter(description = "장소 식별자입니다.", example = "1", required = true) int placeId,
      @Parameter(hidden = true) Authentication authentication);

  /**
   * 장소 스크랩을 취소합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 인증된 회원 정보
   * @return 장소 스크랩 결과
   */
  @Operation(
      summary = "장소 스크랩 취소",
      description = "인증된 회원의 장소 스크랩을 취소합니다. 스크랩하지 않은 상태여도 멱등하게 성공(scrapped=false)을 반환합니다.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "스크랩 취소 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"placeId\":1,\"scrapped\":false,\"scrapCount\":0}}"))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}"))),
    @ApiResponse(
        responseCode = "404",
        description = "장소가 없거나 비활성·삭제 상태",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_NOT_FOUND\",\"message\":\"장소를 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PlaceScrapResponse>> cancelPlaceScrap(
      @Parameter(description = "장소 식별자입니다.", example = "1", required = true) int placeId,
      @Parameter(hidden = true) Authentication authentication);

  /**
   * 장소 좋아요를 생성합니다.
   *
   * @param placeId 장소 식별자
   * @param authentication 인증된 회원 정보
   * @return 장소 좋아요 결과
   */
  @Operation(
      summary = "장소 좋아요 등록",
      description = "인증된 회원이 장소에 좋아요를 누릅니다. 이미 좋아요를 누른 장소이면 400을 반환합니다.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "좋아요 생성 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"placeId\":1,\"liked\":true,\"likeCount\":1}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "이미 좋아요를 누른 장소",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_ALREADY_LIKED\",\"message\":\"이미 좋아요를 눌렀습니다.\"}"))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}"))),
    @ApiResponse(
        responseCode = "404",
        description = "장소가 없거나 비활성·삭제 상태",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"PLACE_NOT_FOUND\",\"message\":\"장소를 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<PlaceLikeResponse>> likePlace(
      @Parameter(description = "장소 식별자입니다.", example = "1", required = true) int placeId,
      @Parameter(hidden = true) Authentication authentication);
}
