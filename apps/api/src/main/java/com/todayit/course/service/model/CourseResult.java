package com.todayit.course.service.model;

import com.todayit.course.entity.Course.CourseSnapshot;
import com.todayit.course.entity.CourseTransport;
import java.time.LocalDateTime;

/**
 * 코스 조회 결과입니다.
 *
 * @param courseId 코스 식별자
 * @param title 코스명
 * @param courseSummary 코스 요약
 * @param transport 이동수단
 * @param startAt 코스 시작 시간
 * @param endAt 코스 종료 시간
 * @param viewCount 조회수
 */
public record CourseResult(
    int courseId,
    String title,
    String courseSummary,
    CourseTransport transport,
    LocalDateTime startAt,
    LocalDateTime endAt,
    int viewCount) {

  /**
   * 코스 Entity의 조회 정보를 서비스 결과로 변환합니다.
   *
   * @param snapshot 코스 조회 정보
   * @return 코스 조회 결과
   */
  public static CourseResult from(CourseSnapshot snapshot) {
    return new CourseResult(
        snapshot.courseId(),
        snapshot.title(),
        snapshot.courseSummary(),
        snapshot.transport(),
        snapshot.startAt(),
        snapshot.endAt(),
        snapshot.viewCount());
  }
}
