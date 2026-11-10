package com.todayit.member.entity;

/**
 * 회원의 선호 지역을 구성하는 행정 지역 코드입니다.
 *
 * @param sidoCode 시도 코드 2자리
 * @param sigunguCode 시군구 코드 3자리
 */
public record PreferredRegionCode(String sidoCode, String sigunguCode) {}
