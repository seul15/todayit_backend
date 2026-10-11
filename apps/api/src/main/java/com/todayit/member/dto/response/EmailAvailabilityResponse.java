package com.todayit.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이메일 중복 확인 응답입니다.
 *
 * @param email 확인한 이메일
 * @param available 사용 가능 여부
 */
@Schema(description = "이메일 중복 확인 응답 데이터")
public record EmailAvailabilityResponse(
    @Schema(description = "중복 확인한 이메일입니다.", example = "user@example.com") String email,
    @Schema(description = "로컬 회원가입에 사용할 수 있으면 true, 사용 중이면 false입니다.", example = "true")
        boolean available) {}
