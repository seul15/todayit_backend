package com.todayit.place.service.model;

import java.time.LocalDateTime;

/**
 * 날짜별 영업시간 조회 결과입니다.
 *
 * @param openDate 영업 날짜
 * @param startAt 시작 시각
 * @param endAt 종료 시각
 * @param breakStartAt 브레이크 시작 시각
 * @param breakEndAt 브레이크 종료 시각
 */
public record PlaceOpeningHoursResult(
    LocalDateTime openDate,
    LocalDateTime startAt,
    LocalDateTime endAt,
    LocalDateTime breakStartAt,
    LocalDateTime breakEndAt) {}
