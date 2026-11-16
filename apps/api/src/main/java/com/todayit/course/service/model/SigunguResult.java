package com.todayit.course.service.model;

/**
 * 시·군·구 조회 결과 모델입니다.
 *
 * @param sigunguCode 시·군·구 코드
 * @param sigunguName 시·군·구 명칭
 */
public record SigunguResult(String sigunguCode, String sigunguName) {}
