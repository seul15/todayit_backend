package com.todayit.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 날짜별 영업시간 응답입니다.
 *
 * @param openDate 영업 날짜
 * @param startAt 시작 시각
 * @param endAt 종료 시각
 * @param breakStartAt 브레이크 시작 시각
 * @param breakEndAt 브레이크 종료 시각
 */
@Schema(description = "장소 영업시간 응답")
public record PlaceOpeningHoursResponse(
    @Schema(
            description = "영업 날짜입니다.",
            example = "2026-10-16T00:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime openDate,
    @Schema(
            description = "영업 시작 시각입니다.",
            example = "2026-10-16T09:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime startAt,
    @Schema(
            description = "영업 종료 시각입니다.",
            example = "2026-10-16T18:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime endAt,
    @Schema(
            description = "브레이크 시작 시각입니다. 브레이크가 없으면 null을 반환합니다.",
            example = "2026-10-16T12:00:00",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
        LocalDateTime breakStartAt,
    @Schema(
            description = "브레이크 종료 시각입니다. 브레이크가 없으면 null을 반환합니다.",
            example = "2026-10-16T13:00:00",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
        LocalDateTime breakEndAt) {}
