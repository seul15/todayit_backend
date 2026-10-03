package com.todayit.place.dto.response;

import com.todayit.place.service.model.PlaceScrapResult;

/**
 * 장소 스크랩 API 응답입니다.
 *
 * @param placeId 장소 식별자
 * @param scrapped 현재 회원의 스크랩 여부
 * @param scrapCount 장소의 활성 스크랩 수
 */
public record PlaceScrapResponse(int placeId, boolean scrapped, long scrapCount) {

  /**
   * 장소 스크랩 결과를 API 응답으로 변환합니다.
   *
   * @param result 장소 스크랩 결과
   * @return 장소 스크랩 응답
   */
  public static PlaceScrapResponse from(PlaceScrapResult result) {
    return new PlaceScrapResponse(result.placeId(), result.scrapped(), result.scrapCount());
  }
}
