package com.todayit.course.service;

import com.todayit.common.pagination.PageResult;
import com.todayit.course.entity.Course;
import com.todayit.course.repository.CourseRepository;
import com.todayit.course.service.model.CourseResult;
import com.todayit.course.service.model.CourseSort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 코스 조회 업무를 처리하는 Query Service입니다. */
@Service
public class CourseQueryService implements CoursePlaceQueryService {

  private final CourseRepository courseRepository;

  /**
   * 코스 조회에 필요한 Repository를 받습니다.
   *
   * @param courseRepository 코스 Repository
   */
  public CourseQueryService(CourseRepository courseRepository) {
    this.courseRepository = courseRepository;
  }

  /**
   * 특정 장소를 포함한 공개 활성 코스를 페이지 단위로 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 코스 정렬 기준
   * @return 장소를 포함한 코스 페이지
   */
  @Transactional(readOnly = true)
  @Override
  public PageResult<CourseResult> findCoursesByPlace(
      int placeId, int page, int size, CourseSort sort) {
    Sort order =
        sort == CourseSort.POPULAR
            ? Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("courseId"))
            : Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("courseId"));
    Page<Course> coursePage =
        courseRepository.findPublicActiveByPlaceId(placeId, PageRequest.of(page, size, order));

    return new PageResult<>(
        coursePage.getContent().stream()
            .map(course -> CourseResult.from(course.getSnapshot()))
            .toList(),
        coursePage.getNumber(),
        coursePage.getSize(),
        coursePage.getTotalElements());
  }
}
