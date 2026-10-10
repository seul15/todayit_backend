package com.todayit.course.dto.response;

import com.todayit.course.entity.CourseTransport;
import com.todayit.course.service.model.CourseResult;
import java.time.LocalDateTime;

/**
 * 코스 조회 API 응답입니다.
 *
 * @param courseId 코스 식별자
 * @param title 코스명
 * @param courseSummary 코스 요약
 * @param transport 이동수단
 * @param startAt 코스 시작 시간
 * @param endAt 코스 종료 시간
 * @param viewCount 조회수
 */
public record CourseResponse(
    int courseId,
    String title,
    String courseSummary,
    CourseTransport transport,
    LocalDateTime startAt,
    LocalDateTime endAt,
    int viewCount) {

  /**
   * 코스 조회 결과를 API 응답으로 변환합니다.
   *
   * @param result 코스 조회 결과
   * @return 코스 API 응답
   */
  public static CourseResponse from(CourseResult result) {
    return new CourseResponse(
        result.courseId(),
        result.title(),
        result.courseSummary(),
        result.transport(),
        result.startAt(),
        result.endAt(),
        result.viewCount());
  }
}
