package com.todayit.course.service.model;

import java.util.List;

/**
 * 시·도 및 하위 시·군·구 목록을 포함하는 행정 지역 조회 결과 모델입니다.
 *
 * @param sidoCode 시·도 코드
 * @param sidoName 시·도 명칭
 * @param sigungus 하위 시·군·구 결과 목록
 */
public record RegionResult(String sidoCode, String sidoName, List<SigunguResult> sigungus) {}
