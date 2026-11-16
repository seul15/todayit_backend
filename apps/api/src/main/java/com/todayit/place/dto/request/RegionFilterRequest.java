package com.todayit.place.dto.request;

import com.todayit.course.service.model.RegionFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 장소 필터용 지역 요청 DTO입니다.
 *
 * @param sidoCode 시·도 코드 2자리
 * @param sigunguCodes 시·군·구 코드 3자리 목록 (시도당 최대 3개, 생략 시 해당 시도 전체)
 */
@Schema(description = "장소 지역 필터 조건")
public record RegionFilterRequest(
    @Schema(
            description = "시·도 코드 2자리입니다.",
            example = "11",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String sidoCode,
    @Schema(
            description =
                "시·군·구 코드 3자리 목록입니다. 시도당 최대 3개까지 선택할 수 있으며, 빈 배열이거나 생략하면 해당 시도 전체를 조회합니다.",
            example = "[\"680\", \"650\"]")
        List<String> sigunguCodes) {

  /**
   * 시·군·구 코드 목록이 null인 경우 빈 불변 리스트로 초기화합니다.
   *
   * @param sidoCode 시·도 코드 2자리
   * @param sigunguCodes 시·군·구 코드 3자리 목록
   */
  public RegionFilterRequest {
    if (sigunguCodes == null) {
      sigunguCodes = List.of();
    }
  }

  /**
   * 서비스 계층 모델로 변환합니다.
   *
   * @return 지역 필터 모델
   */
  public RegionFilter toModel() {
    return new RegionFilter(sidoCode, sigunguCodes);
  }
}
