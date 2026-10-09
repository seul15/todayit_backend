package com.todayit.member.dto.response;

/**
 * 닉네임 중복 확인 응답입니다.
 *
 * @param available 사용 가능 여부
 */
public record NicknameAvailabilityResponse(boolean available) {}
