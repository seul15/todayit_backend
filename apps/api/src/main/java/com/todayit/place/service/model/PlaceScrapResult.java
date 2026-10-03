package com.todayit.place.service.model;

/**
 * 장소 스크랩 결과입니다.
 *
 * @param placeId 장소 식별자
 * @param scrapped 현재 회원의 스크랩 여부
 * @param scrapCount 장소의 활성 스크랩 수
 */
public record PlaceScrapResult(int placeId, boolean scrapped, long scrapCount) {}
