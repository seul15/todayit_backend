package com.todayit.place.service;

import com.todayit.common.pagination.PageResult;
import com.todayit.place.service.model.PlaceResult;
import com.todayit.place.service.model.PlaceScrapSort;

/** 회원의 스크랩 장소 조회에 필요한 장소 서비스 계약입니다. */
public interface PlaceScrapQueryService {

  /**
   * 회원이 활성 상태로 스크랩한 장소를 페이지 단위로 조회합니다.
   *
   * @param memberId 회원 식별자
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 스크랩 장소 정렬 기준
   * @return 스크랩 장소 페이지
   */
  PageResult<PlaceResult> findScrappedPlaces(
      String memberId, int page, int size, PlaceScrapSort sort);
}
