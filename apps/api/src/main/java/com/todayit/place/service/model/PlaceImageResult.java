package com.todayit.place.service.model;

/**
 * 장소 사진 조회 결과입니다.
 *
 * @param placeImageId 장소 사진 식별자
 * @param imageUrl 장소 사진 URL
 */
public record PlaceImageResult(int placeImageId, String imageUrl) {}
