package com.todayit.place.dto.response;

import com.todayit.place.service.model.PlaceLikeResult;

/**
 * 장소 좋아요 API 응답입니다.
 *
 * @param placeId 장소 식별자
 * @param liked 현재 회원의 좋아요 여부
 * @param likeCount 장소의 좋아요 수
 */
public record PlaceLikeResponse(int placeId, boolean liked, long likeCount) {

  /**
   * 장소 좋아요 결과를 API 응답으로 변환합니다.
   *
   * @param result 장소 좋아요 결과
   * @return 장소 좋아요 응답
   */
  public static PlaceLikeResponse from(PlaceLikeResult result) {
    return new PlaceLikeResponse(result.placeId(), result.liked(), result.likeCount());
  }
}
