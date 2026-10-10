package com.todayit.course.service;

import com.todayit.common.pagination.PageResult;
import com.todayit.course.service.model.CourseResult;
import com.todayit.course.service.model.CourseSort;

/** 장소 기능에서 코스 정보를 조회하기 위한 서비스 계약입니다. */
public interface CoursePlaceQueryService {

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
