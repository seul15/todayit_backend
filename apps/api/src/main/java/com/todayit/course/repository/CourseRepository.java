package com.todayit.course.repository;

import com.todayit.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 코스 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface CourseRepository extends JpaRepository<Course, Integer> {

  /**
   * 특정 장소를 포함한 공개 활성 코스를 페이지 단위로 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param pageable 페이지와 정렬 조건
   * @return 장소를 포함한 코스 페이지
   */
  @Query(
      value =
          "select distinct course from Course course "
              + "join course.coursePlaces coursePlace "
              + "where coursePlace.placeId = :placeId "
              + "and course.status = com.todayit.course.entity.CourseStatus.ACTIVE "
              + "and course.visibility = com.todayit.course.entity.CourseVisibility.PUBLIC",
      countQuery =
          "select count(distinct course) from Course course "
              + "join course.coursePlaces coursePlace "
              + "where coursePlace.placeId = :placeId "
              + "and course.status = com.todayit.course.entity.CourseStatus.ACTIVE "
              + "and course.visibility = com.todayit.course.entity.CourseVisibility.PUBLIC")
  Page<Course> findPublicActiveByPlaceId(@Param("placeId") int placeId, Pageable pageable);
}
