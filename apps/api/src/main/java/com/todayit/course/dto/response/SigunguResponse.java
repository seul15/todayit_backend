package com.todayit.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시·군·구 정보 응답 DTO입니다.
 *
 * @param sigunguCode 시·군·구 코드
 * @param sigunguName 시·군·구 명칭
 */
@Schema(description = "시·군·구 응답")
public record SigunguResponse(
    @Schema(description = "시·군·구 법정동 코드 (3자리)", example = "680") String sigunguCode,
    @Schema(description = "시·군·구 명칭", example = "강남구") String sigunguName) {}
