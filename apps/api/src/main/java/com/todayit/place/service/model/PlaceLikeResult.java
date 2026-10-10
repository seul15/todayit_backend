package com.todayit.place.service.model;

/**
 * 장소 좋아요 결과입니다.
 *
 * @param placeId 장소 식별자
 * @param liked 현재 회원의 좋아요 여부
 * @param likeCount 장소의 좋아요 수
 */
public record PlaceLikeResult(int placeId, boolean liked, long likeCount) {}
