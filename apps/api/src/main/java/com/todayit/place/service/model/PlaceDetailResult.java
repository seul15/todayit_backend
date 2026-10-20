package com.todayit.place.service.model;

import java.util.List;

/**
 * 장소 상세 조회 결과입니다.
 *
 * @param place 장소 기본 정보
 * @param description 장소 설명
 * @param openingHours 영업시간 목록
 * @param liked 좋아요 여부
 * @param scrapped 스크랩 여부
 */
public record PlaceDetailResult(
    PlaceResult place,
    String description,
    List<PlaceOpeningHoursResult> openingHours,
    boolean liked,
    boolean scrapped) {
  /**
   * 영업시간 목록을 방어적으로 복사합니다.
   *
   * @param place 장소 기본 정보
   * @param description 장소 설명
   * @param openingHours 영업시간 목록
   * @param liked 좋아요 여부
   * @param scrapped 스크랩 여부
   */
  public PlaceDetailResult {
    openingHours = List.copyOf(openingHours);
  }
}
