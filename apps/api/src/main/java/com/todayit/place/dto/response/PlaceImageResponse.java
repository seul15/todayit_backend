package com.todayit.place.dto.response;

import com.todayit.place.service.model.PlaceImageResult;

/**
 * 장소 사진 응답입니다.
 *
 * @param placeImageId 장소 사진 식별자
 * @param imageUrl 장소 사진 URL
 */
public record PlaceImageResponse(int placeImageId, String imageUrl) {

  /**
   * 장소 사진 결과를 API 응답으로 변환합니다.
   *
   * @param result 장소 사진 결과
   * @return 장소 사진 응답
   */
  public static PlaceImageResponse from(PlaceImageResult result) {
    return new PlaceImageResponse(result.placeImageId(), result.imageUrl());
  }
}
