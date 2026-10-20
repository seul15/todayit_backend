package com.todayit.course.service;

import com.todayit.common.pagination.PageResult;
import com.todayit.course.service.model.CourseResult;
import com.todayit.course.service.model.CourseSort;

/** 장소 기능에서 코스 정보를 조회하기 위한 서비스 계약입니다. */
public interface CoursePlaceQueryService {
  /**
   * 공개 여부와 관계없이 활성 코스에서 0분을 제외한 장소의 평균 체류 시간을 분 단위 정수로 반올림합니다.
   *
   * @param placeId 장소 식별자
   * @return 평균 체류 시간(분), 데이터가 없으면 0
   */
  long getAverageStayMinutes(int placeId);

  /**
   * 특정 장소를 포함한 공개 활성 코스를 페이지 단위로 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 코스 정렬 기준
   * @return 장소를 포함한 코스 페이지
   */
  PageResult<CourseResult> findCoursesByPlace(int placeId, int page, int size, CourseSort sort);
}
