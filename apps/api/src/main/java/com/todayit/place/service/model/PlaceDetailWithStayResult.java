package com.todayit.place.service.model;

/**
 * 평균 체류 시간을 포함한 장소 상세 결과입니다.
 *
 * @param detail 장소 상세 정보
 * @param averageStayMinutes 평균 체류 시간(분), 데이터가 없으면 0
 */
public record PlaceDetailWithStayResult(PlaceDetailResult detail, long averageStayMinutes) {}
