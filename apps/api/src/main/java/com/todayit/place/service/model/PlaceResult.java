package com.todayit.place.service.model;

import com.todayit.place.entity.Category;
import com.todayit.place.entity.Place.PlaceSnapshot;
import java.math.BigDecimal;
import java.util.List;

/**
 * 장소 목록에서 공개할 장소 정보입니다.
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
public record PlaceResult(
    int placeId,
    String name,
    BigDecimal latitude,
    BigDecimal longitude,
    String address,
    Category category,
    int viewCount,
    List<String> imageUrls) {

  /**
   * 장소 Entity의 조회 정보를 서비스 결과로 변환합니다.
   *
   * @param snapshot 장소 조회 정보
   * @return 장소 서비스 결과
   */
  public static PlaceResult from(PlaceSnapshot snapshot) {
    return new PlaceResult(
        snapshot.placeId(),
        snapshot.name(),
        snapshot.latitude(),
        snapshot.longitude(),
        snapshot.address(),
        snapshot.category(),
        snapshot.viewCount(),
        snapshot.imageUrls());
  }
}
