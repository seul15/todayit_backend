package com.todayit.place.service;

import com.todayit.course.service.CoursePlaceQueryService;
import com.todayit.place.service.model.PlaceDetailResult;
import com.todayit.place.service.model.PlaceDetailWithStayResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 장소 상세와 코스의 평균 체류 시간을 조합합니다. */
@Service
public class PlaceDetailFacade {

  private static final Logger LOG = LoggerFactory.getLogger(PlaceDetailFacade.class);
  private final PlaceDetailQueryService places;
  private final CoursePlaceQueryService courses;

  /**
   * 장소와 코스 조회 서비스를 주입합니다.
   *
   * @param places 장소 조회
   * @param courses 코스 조회
   */
  public PlaceDetailFacade(PlaceDetailQueryService places, CoursePlaceQueryService courses) {
    this.places = places;
    this.courses = courses;
  }

  /**
   * 장소가 조회 가능한 상태인지 확인한 후 평균 체류 시간을 조합합니다.
   *
   * @param placeId 장소 식별자
   * @param memberId 회원 식별자, 비회원이면 null
   * @return 조회 결과
   */
  @Transactional(readOnly = true)
  public PlaceDetailWithStayResult getPlaceDetail(int placeId, String memberId) {
    PlaceDetailResult detail = places.getPlaceDetail(placeId, memberId);
    long averageStayMinutes = courses.getAverageStayMinutes(placeId);
    LOG.info(
        "장소 상세 조회: placeId={}, authenticated={}, openingHoursCount={}, imageCount={}",
        placeId,
        memberId != null,
        detail.openingHours().size(),
        detail.place().imageUrls().size());
    return new PlaceDetailWithStayResult(detail, averageStayMinutes);
  }
}
