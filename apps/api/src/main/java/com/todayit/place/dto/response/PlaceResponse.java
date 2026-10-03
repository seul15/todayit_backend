package com.todayit.place.dto.response;

import com.todayit.place.entity.Category;
import com.todayit.place.service.model.PlaceResult;
import java.math.BigDecimal;
import java.util.List;

/**
 * 장소 목록에서 반환할 장소 응답입니다.
 *
 * @param placeId 장소 식별자
 * @param name 장소명
 * @param latitude 위도
 * @param longitude 경도
 * @param address 주소
 * @param category 카테고리
 * @param viewCount 조회수
 * @param imageUrls 장소 이미지 URL 목록
 */
public record PlaceResponse(
    int placeId,
    String name,
    BigDecimal latitude,
    BigDecimal longitude,
    String address,
    Category category,
    int viewCount,
    List<String> imageUrls) {

  /**
   * 장소 조회 결과를 API 응답으로 변환합니다.
   *
   * @param result 장소 조회 결과
   * @return 장소 응답
   */
  public static PlaceResponse from(PlaceResult result) {
    return new PlaceResponse(
        result.placeId(),
        result.name(),
        result.latitude(),
        result.longitude(),
        result.address(),
        result.category(),
        result.viewCount(),
        result.imageUrls());
  }
}
