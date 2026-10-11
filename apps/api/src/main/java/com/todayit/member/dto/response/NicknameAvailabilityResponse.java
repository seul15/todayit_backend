package com.todayit.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 닉네임 중복 확인 응답입니다.
 *
 * @param available 사용 가능 여부
 */
@Schema(description = "닉네임 중복 확인 응답 데이터")
public record NicknameAvailabilityResponse(
    @Schema(description = "사용 가능한 닉네임이면 true, 이미 사용 중이면 false입니다.", example = "true")
        boolean available) {}
