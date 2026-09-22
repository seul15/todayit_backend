package com.todayit.member.dto.response;

/**
 * 이메일 중복 확인 응답입니다.
 *
 * @param email 확인한 이메일
 * @param available 사용 가능 여부
 */
public record EmailAvailabilityResponse(String email, boolean available) {}
