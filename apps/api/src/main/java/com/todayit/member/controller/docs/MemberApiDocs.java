package com.todayit.member.controller.docs;

import com.todayit.common.openapi.OpenApiConfig;
import com.todayit.common.pagination.PageResponse;
import com.todayit.common.response.CommonResponse;
import com.todayit.common.response.ErrorResponse;
import com.todayit.member.dto.request.MemberProfileRequest;
import com.todayit.member.dto.response.MemberProfileDetailResponse;
import com.todayit.member.dto.response.MemberProfileImageResponse;
import com.todayit.member.dto.response.MemberProfileResponse;
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
import org.springframework.web.multipart.MultipartFile;

/** 회원의 닉네임, 스크랩 장소 및 프로필 관련 API 명세입니다. */
@Tag(name = "회원", description = "닉네임 확인, 스크랩 장소 및 회원 프로필 API")
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
          "닉네임 앞뒤 공백을 제거한 후 길이(2~8자), 허용 문자(한글·영문·숫자), "
              + "금칙어를 검사하고 사용 가능 여부를 반환합니다. "
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
                examples = {
                  @ExampleObject(
                      name = "닉네임 누락",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"닉네임은 필수입니다.\"}"),
                  @ExampleObject(
                      name = "닉네임 길이 오류",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"닉네임은 2자 이상 8자 이하이어야 합니다.\"}"),
                  @ExampleObject(
                      name = "닉네임 형식 오류",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.\"}"),
                  @ExampleObject(
                      name = "금칙어",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"사용할 수 없는 닉네임입니다.\"}")
                }))
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

  /**
   * 인증된 회원의 프로필 이미지를 등록하거나 교체합니다.
   *
   * @param file 업로드할 이미지 파일
   * @param authentication 인증된 회원 정보
   * @return 변경된 프로필 이미지 정보
   */
  @Operation(
      summary = "프로필 이미지 등록 및 변경",
      description =
          "로그인한 회원의 프로필 이미지를 등록하거나 교체합니다. "
              + "PROFILE_IMAGE 약관에 동의한 회원만 사용할 수 있습니다. "
              + "이미 등록된 이미지가 있으면 새 이미지로 교체합니다.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "프로필 이미지 등록 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "파일 검증 오류 또는 프로필 이미지 약관 미동의",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                  @ExampleObject(
                      name = "잘못된 multipart 요청",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"),
                  @ExampleObject(
                      name = "약관 미동의",
                      value =
                          "{\"success\":false,\"code\":\"PROFILE_IMAGE_CONSENT_REQUIRED\",\"message\":\"프로필 이미지 등록을 위한 약관 동의가 필요합니다.\"}"),
                  @ExampleObject(
                      name = "파일명 오류",
                      value =
                          "{\"success\":false,\"code\":\"FILE_INVALID_NAME\",\"message\":\"유효한 원본 파일명이 필요합니다.\"}"),
                  @ExampleObject(
                      name = "파일 크기 오류",
                      value =
                          "{\"success\":false,\"code\":\"FILE_INVALID_SIZE\",\"message\":\"파일 크기가 허용 범위를 벗어났습니다.\"}"),
                  @ExampleObject(
                      name = "확장자 오류",
                      value =
                          "{\"success\":false,\"code\":\"FILE_UNSUPPORTED_EXTENSION\",\"message\":\"허용 확장자는 jpg/jpeg/png/webp/pdf입니다.\"}"),
                  @ExampleObject(
                      name = "프로필 PDF 업로드",
                      value =
                          "{\"success\":false,\"code\":\"FILE_PROFILE_IMAGE_REQUIRED\",\"message\":\"프로필에는 이미지만 업로드할 수 있습니다.\"}"),
                  @ExampleObject(
                      name = "MIME 불일치",
                      value =
                          "{\"success\":false,\"code\":\"FILE_MIME_MISMATCH\",\"message\":\"확장자와 MIME이 일치하지 않습니다.\"}"),
                  @ExampleObject(
                      name = "실제 파일 형식 불일치",
                      value =
                          "{\"success\":false,\"code\":\"FILE_CONTENT_MISMATCH\",\"message\":\"파일의 실제 형식이 확장자 및 MIME과 일치하지 않습니다.\"}")
                })),
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
                            "{\"success\":false,\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"인증이 필요합니다.\"}"))),
    @ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"MEMBER_NOT_FOUND\",\"message\":\"회원을 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "파일 저장소 처리 실패",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                  @ExampleObject(
                      name = "파일 읽기 실패",
                      value =
                          "{\"success\":false,\"code\":\"FILE_READ_FAILED\",\"message\":\"업로드 파일을 읽을 수 없습니다.\"}"),
                  @ExampleObject(
                      name = "파일 저장소 오류",
                      value =
                          "{\"success\":false,\"code\":\"FILE_STORAGE_FAILED\",\"message\":\"파일 저장소 작업에 실패했습니다.\"}")
                }))
  })
  ResponseEntity<CommonResponse<MemberProfileImageResponse>> uploadProfileImage(
      MultipartFile file, Authentication authentication);

  /**
   * 로그인한 회원의 프로필 정보를 수정합니다.
   *
   * @param request 회원 프로필 수정 요청
   * @param authentication 인증된 회원 정보
   * @return 변경된 회원 프로필 정보
   */
  @Operation(
      summary = "회원 프로필 수정",
      description =
          "로그인한 회원의 닉네임, 선호 지역 및 선호 테마를 수정합니다. "
              + "요청에서 생략한 항목은 기존 값을 유지합니다. "
              + "preferredRegions는 최대 1개까지 선택할 수 있으며, "
              + "각 지역은 sidoCode와 sigunguCode로 구성됩니다. "
              + "빈 배열을 전달하면 기존 선호 지역을 모두 해제합니다. "
              + "preferredThemes에 빈 배열을 전달하면 선호 테마를 모두 해제합니다. "
              + "닉네임은 마지막 변경 이후 90일이 지나야 다시 변경할 수 있습니다.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "회원 프로필 수정 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":"
                                + "{\"memberId\":\"123e4567-e89b-12d3-a456-426614174000\","
                                + "\"nickname\":\"새닉네임\","
                                + "\"profileImage\":null,"
                                + "\"preferredRegions\":[{\"sidoCode\":\"11\","
                                + "\"sigunguCode\":\"110\"}],"
                                + "\"preferredThemes\":[\"QUIET\",\"ROMANTIC\"],"
                                + "\"updatedAt\":\"2026-11-09T04:00:00\"}}"))),
    @ApiResponse(
        responseCode = "400",
        description = "입력값 오류, 선호 지역 개수 초과 또는 닉네임 변경 주기 위반",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                  @ExampleObject(
                      name = "수정 항목 누락",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\","
                              + "\"message\":\"수정할 회원정보가 없습니다.\"}"),
                  @ExampleObject(
                      name = "선호 지역 개수 초과",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REQUEST\","
                              + "\"message\":\"선호 지역은 1개만 선택할 수 있습니다.\"}"),
                  @ExampleObject(
                      name = "닉네임 변경 주기 위반",
                      value =
                          "{\"success\":false,\"code\":\"NICKNAME_CHANGE_TOO_SOON\","
                              + "\"message\":\"닉네임은 마지막 변경 후 90일이 지나야 다시 변경할 수 있습니다.\"}"),
                  @ExampleObject(
                      name = "존재하지 않는 선호 지역",
                      value =
                          "{\"success\":false,\"code\":\"INVALID_REGION_PATH\","
                              + "\"message\":\"지역 필터 값이 올바르지 않습니다.\"}")
                })),
    @ApiResponse(responseCode = "401", description = "Access Token 인증 필요"),
    @ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"MEMBER_NOT_FOUND\","
                                + "\"message\":\"회원을 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "409",
        description = "닉네임 중복",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"NICKNAME_ALREADY_EXISTS\","
                                + "\"message\":\"이미 사용 중인 닉네임입니다.\"}")))
  })
  ResponseEntity<CommonResponse<MemberProfileResponse>> updateProfile(
      MemberProfileRequest request, Authentication authentication);

  /**
   * 로그인한 회원 본인의 프로필 정보를 조회합니다.
   *
   * @param authentication 인증된 회원 정보
   * @return 회원 프로필 조회 응답
   */
  @Operation(
      summary = "회원 프로필 조회",
      description =
          "로그인한 회원 본인의 프로필 정보를 조회합니다. "
              + "이메일은 본인 조회에서만 반환하며, "
              + "소셜 회원의 이메일은 null입니다. "
              + "등록한 프로필 이미지가 없으면 null을 반환하고, "
              + "선호 지역과 테마가 설정되지 않았으면 빈 배열을 반환합니다.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "회원 프로필 조회 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":"
                                + "{\"memberId\":\"123e4567-e89b-12d3-a456-426614174000\","
                                + "\"email\":\"test@example.com\","
                                + "\"nickname\":\"테스트회원\","
                                + "\"profileImage\":null,"
                                + "\"preferredRegions\":[],"
                                + "\"preferredThemes\":[],"
                                + "\"updatedAt\":\"2026-11-09T04:00:00\"}}"))),
    @ApiResponse(responseCode = "401", description = "Access Token 인증 필요"),
    @ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CommonResponse<MemberProfileDetailResponse>> findProfile(
      Authentication authentication);

  /**
   * 로그인한 회원의 프로필 이미지를 삭제합니다.
   *
   * @param authentication 인증된 회원 정보
   * @return 기본 이미지로 복원된 회원 프로필 정보
   */
  @Operation(
      summary = "프로필 이미지 삭제",
      description =
          "로그인한 회원 본인의 프로필 이미지를 삭제하고 기본 이미지로 복원합니다. "
              + "별도의 요청 본문은 필요하지 않습니다. "
              + "삭제 후 profileImage는 null로 반환하며, 프론트에서 기본 이미지를 표시합니다. "
              + "이미 기본 이미지인 경우에도 성공하고 기존 updatedAt을 유지합니다. "
              + "닉네임과 선호 정보는 변경하지 않습니다.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "프로필 이미지 삭제 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":true,\"data\":"
                                + "{\"memberId\":\"123e4567-e89b-12d3-a456-426614174000\","
                                + "\"profileImage\":null,"
                                + "\"updatedAt\":\"2026-11-10T05:00:00\"}}"))),
    @ApiResponse(responseCode = "401", description = "Access Token 인증 필요"),
    @ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,\"code\":\"MEMBER_NOT_FOUND\","
                                + "\"message\":\"회원을 찾을 수 없습니다.\"}"))),
    @ApiResponse(
        responseCode = "500",
        description = "외부 파일 저장소 삭제 실패",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"success\":false,"
                                + "\"code\":\"FILE_STORAGE_FAILED\","
                                + "\"message\":\"파일 저장소 작업에 실패했습니다.\"}")))
  })
  ResponseEntity<CommonResponse<MemberProfileImageResponse>> deleteProfileImage(
      Authentication authentication);
}
