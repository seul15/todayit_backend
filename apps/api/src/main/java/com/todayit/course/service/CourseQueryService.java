package com.todayit.course.service;

import com.todayit.common.pagination.PageResult;
import com.todayit.course.entity.Course;
import com.todayit.course.repository.CourseRepository;
import com.todayit.course.service.model.CourseResult;
import com.todayit.course.service.model.CourseSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 코스 조회 업무를 처리하는 Query Service입니다. */
@Service
public class CourseQueryService implements CoursePlaceQueryService {

  private static final Logger LOG = LoggerFactory.getLogger(CourseQueryService.class);

  private final CourseRepository courseRepository;

  /**
   * 공개 여부와 관계없이 해당 장소가 포함된 활성 코스에서 0분을 제외한 체류 시간을 평균 내어 분 단위 정수로 반올림합니다.
   *
   * @param placeId 장소 식별자
   * @return 평균 체류 시간(분), 0분을 제외한 체류 시간이 없으면 0
   */
  @Override
  @Transactional(readOnly = true)
  public long getAverageStayMinutes(int placeId) {
    Double average = courseRepository.findAverageStayMinutes(placeId);
    return average == null ? 0 : Math.round(average);
  }

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

    LOG.info(
        "장소별 코스 조회: placeId={}, page={}, size={}, sort={}, resultCount={}",
        placeId,
        page,
        size,
        sort,
        coursePage.getNumberOfElements());
    return new PageResult<>(
        coursePage.getContent().stream()
            .map(course -> CourseResult.from(course.getSnapshot()))
            .toList(),
        coursePage.getNumber(),
        coursePage.getSize(),
        coursePage.getTotalElements());
  }
}
