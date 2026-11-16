package com.todayit.course.service.model;

import java.util.List;

/**
 * 지역 필터 조건 모델입니다.
 *
 * @param sidoCode 시·도 코드 2자리
 * @param sigunguCodes 시·군·구 코드 3자리 목록, 비어있으면 해당 시도 전체
 */
public record RegionFilter(String sidoCode, List<String> sigunguCodes) {

  /**
   * 시·군·구 코드 목록이 null인 경우 빈 불변 리스트로 초기화합니다.
   *
   * @param sidoCode 시·도 코드 2자리
   * @param sigunguCodes 시·군·구 코드 3자리 목록
   */
  public RegionFilter {
    if (sigunguCodes == null) {
      sigunguCodes = List.of();
    }
  }
}
