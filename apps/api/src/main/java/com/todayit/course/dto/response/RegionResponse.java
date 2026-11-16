package com.todayit.course.dto.response;

import com.todayit.course.service.model.RegionResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 시·도 및 하위 시·군·구 목록을 포함하는 행정 지역 응답 DTO입니다.
 *
 * @param sidoCode 시·도 코드
 * @param sidoName 시·도 명칭
 * @param sigungus 하위 시·군·구 목록
 */
@Schema(description = "시·도 및 시·군·구 계층 지역 응답")
public record RegionResponse(
    @Schema(description = "시·도 법정동 코드 (2자리)", example = "11") String sidoCode,
    @Schema(description = "시·도 명칭", example = "서울특별시") String sidoName,
    @Schema(description = "하위 시·군·구 목록") List<SigunguResponse> sigungus) {

  /**
   * 서비스 계층의 지역 모델로부터 응답 DTO를 생성합니다.
   *
   * @param result 지역 조회 결과 모델
   * @return 지역 응답 DTO
   */
  public static RegionResponse from(RegionResult result) {
    List<SigunguResponse> sigunguResponses =
        result.sigungus().stream()
            .map(s -> new SigunguResponse(s.sigunguCode(), s.sigunguName()))
            .toList();
    return new RegionResponse(result.sidoCode(), result.sidoName(), sigunguResponses);
  }
}
