package com.todayit.place.service.model;

import com.todayit.place.entity.VisitorInfoType;
import java.util.List;
import java.util.Map;

/**
 * 장소 상세 조회 결과입니다.
 *
 * @param place 장소 기본 정보
 * @param description 장소 설명
 * @param openingHours 영업시간 목록
 * @param liked 좋아요 여부
 * @param scrapped 스크랩 여부
 * @param visitorInfo 항목 코드별 편의시설·운영 안내
 */
public record PlaceDetailResult(
    PlaceResult place,
    String description,
    List<PlaceOpeningHoursResult> openingHours,
    boolean liked,
    boolean scrapped,
    Map<VisitorInfoType, String> visitorInfo) {
  /**
   * 영업시간 목록을 방어적으로 복사합니다.
   *
   * @param place 장소 기본 정보
   * @param description 장소 설명
   * @param openingHours 영업시간 목록
   * @param liked 좋아요 여부
   * @param scrapped 스크랩 여부
   * @param visitorInfo 항목 코드별 편의시설·운영 안내
   */
  public PlaceDetailResult {
    openingHours = List.copyOf(openingHours);
    visitorInfo = Map.copyOf(visitorInfo);
  }
}
