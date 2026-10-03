package com.todayit.place.service.model;

import com.todayit.place.entity.Place.PlaceSnapshot;
import java.math.BigDecimal;

/**
 * 장소 지도 조회 결과입니다.
 *
 * @param placeId 장소 식별자
 * @param latitude 위도
 * @param longitude 경도
 * @param address 주소
 */
public record PlaceLocationResult(
    int placeId, BigDecimal latitude, BigDecimal longitude, String address) {

  /**
   * 장소 Entity의 조회 정보를 지도 조회 결과로 변환합니다.
   *
   * @param snapshot 장소 조회 정보
   * @return 장소 지도 조회 결과
   */
  public static PlaceLocationResult from(PlaceSnapshot snapshot) {
    return new PlaceLocationResult(
        snapshot.placeId(), snapshot.latitude(), snapshot.longitude(), snapshot.address());
  }
}
