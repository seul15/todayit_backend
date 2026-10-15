package com.todayit.member.controller.docs;

import com.todayit.common.openapi.OpenApiConfig;
import com.todayit.common.pagination.PageResponse;
import com.todayit.common.response.CommonResponse;
import com.todayit.common.response.ErrorResponse;
import com.todayit.member.dto.response.NicknameAvailabilityResponse;
import com.todayit.place.dto.response.PlaceResponse;
import com.todayit.place.service.model.PlaceScrapSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

/** 회원의 닉네임 확인과 스크랩 장소 조회 API 명세입니다. */
@Tag(name = "회원", description = "닉네임 사용 가능 여부 및 내 스크랩 장소 조회 API")
public interface MemberApiDocs {

  /**
   * 닉네임의 사용 가능 여부를 확인합니다.
   *
   * @param nickname 확인할 닉네임
   * @return 닉네임 사용 가능 여부
   */
  @Operation(
      summary = "닉네임 중복 확인",
      description =
          "닉네임의 길이(2~8자), 허용 문자(한글·영문·숫자), 금칙어를 검사하고 사용 가능 여부를 반환합니다. "
              + "이미 사용 중인 닉네임은 오류가 아닌 available=false로 반환합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "닉네임 확인 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                  @ExampleObject(
                      name = "사용 가능",
                      value = "{\"success\":true,\"data\":{\"available\":true}}"),
                  @ExampleObject(
                      name = "이미 사용 중",
                      value = "{\"success\":true,\"data\":{\"available\":false}}")
                })),
    @ApiResponse(
        responseCode = "400",
        description = "닉네임 누락, 형식 오류 또는 금칙어 사용",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"닉네임은 2자 이상 8자 이하이어야 합니다.\"}")))
  })
  ResponseEntity<CommonResponse<NicknameAvailabilityResponse>> checkNickname(String nickname);

  /**
   * 인증된 회원이 스크랩한 장소를 페이지 단위로 조회합니다.
   *
   * @param page 페이지 번호 (기본값 0)
   * @param sort 스크랩 정렬 기준 (기본값 LATEST)
   * @param authentication 인증된 회원 정보
   * @return 스크랩 장소 목록과 페이지 정보
   */
  @Operation(
      summary = "내 스크랩 장소 조회",
      description =
          "로그인한 회원이 스크랩한 장소를 조회합니다. "
              + "page는 0부터 시작하고 기본값은 0이며, 페이지 크기는 20으로 고정됩니다. "
              + "sort는 LATEST(최근 스크랩순, 기본값) 또는 OLDEST(오래된 스크랩순)입니다.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "스크랩 장소 조회 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":{\"content\":[{\"placeId\":1,\"name\":\"오늘의 식당\",\"latitude\":37.57,\"longitude\":126.985,\"address\":\"서울특별시 종로구 종로 1\",\"category\":\"RESTAURANT\",\"viewCount\":15,\"imageUrls\":[\"https://example.com/place.jpg\"]}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 페이지 요청",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"INVALID_PAGINATION\",\"message\":\"페이지 요청값이 올바르지 않습니다.\"}"))),
    @ApiResponse(
        responseCode = "401",
        description = "Access Token 인증 필요",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"인증이 필요합니다.\"}")))
  })
  ResponseEntity<CommonResponse<PageResponse<PlaceResponse>>> findScrappedPlaces(
      int page, PlaceScrapSort sort, Authentication authentication);
}
